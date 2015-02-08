package com.example.pianonerd77.sandbox;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.provider.Telephony;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.OnInvitationReceivedListener;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateListener;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.plus.Plus;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class OpenScreen extends ActionBarActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, RoomStatusUpdateListener, RoomUpdateListener,
        OnInvitationReceivedListener,RealTimeMessageReceivedListener
{

    private final static int REQUEST_SELECT_PLAYERS = 1000;

    private final static int REQUEST_WAITING_ROOM = 1002;

    private final static int REQUEST_INVITATION_INBOX = 1003;
    private GoogleApiClient googleApiClient;

    // are we already playing?
    boolean mPlaying = false;

    // at least 2 players required for our game
    final static int MIN_PLAYERS = 2;

    private String mRoomId = null;
    private int id = 0;
    private ArrayList<Participant> players = null;
    private String mMyId = null;
    public void onClickButtonListener() {

        Button button_click = (Button) findViewById(R.id.button);
        button_click.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent
                                ("com.example.pianonerd77.sandbox.SelectionPage");
                        startActivity(intent);
                    }
                }
        );
    }

    private void invitePlayers(){
        Intent intent = Games.RealTimeMultiplayer.getSelectOpponentsIntent(googleApiClient, 1, 3);
        startActivityForResult(intent, REQUEST_SELECT_PLAYERS);
    }

    @Override
    public void onActivityResult (int request, int response, Intent data){
        switch (request) {
            case REQUEST_SELECT_PLAYERS:
                // we got the result from the "select players" UI -- ready to create the room
                doSelectPlayerResult(request, data);
                break;
            case REQUEST_WAITING_ROOM:
                // we got the result from the "waiting room" UI.
                if (response == Activity.RESULT_OK) {
                    // ready to start playing
                    startGame();
                } else if (response == GamesActivityResultCodes.RESULT_LEFT_ROOM) {
                    // player indicated that they want to leave the room
                    leaveRoom();
                } else if (response == Activity.RESULT_CANCELED) {
                    // Dialog was cancelled (user pressed back key, for instance). In our game,
                    // this means leaving the room too. In more elaborate games, this could mean
                    // something else (like minimizing the waiting room UI).
                    leaveRoom();
                }
                break;
        }
    }

    private void doSelectPlayerResult(int response, Intent data){
        if (response != Activity.RESULT_OK){
            return; //response is either canceled or first, either of which are not okay
        }

        // get the invitee list
        Bundle extras = data.getExtras();
        final ArrayList<String> invitees =
                data.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);

        // create the room and specify a variant if appropriate
        RoomConfig.Builder roomConfigBuilder = makeBasicRoomConfigBuilder();
        roomConfigBuilder.addPlayersToInvite(invitees);

        RoomConfig roomConfig = roomConfigBuilder.build();
        Games.RealTimeMultiplayer.create(googleApiClient, roomConfig);

        // prevent screen from sleeping during handshake
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    // create a RoomConfigBuilder that's appropriate for your implementation
    private RoomConfig.Builder makeBasicRoomConfigBuilder() {
        return RoomConfig.builder(this)
                .setMessageReceivedListener(this)
                .setRoomStatusUpdateListener(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_screen);

        onClickButtonListener();
        googleApiClient = new GoogleApiClient.Builder(this).
                addConnectionCallbacks(this).
                addOnConnectionFailedListener(this).
                addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN).
                addApi(Games.API).addScope(Games.SCOPE_GAMES).
                build();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_open_screen, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRoomConnected(int statusCode, Room room) {
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            // let screen go to sleep
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            // show error message, return to main screen.

        }
    }


    // returns whether there are enough players to start the game
    private boolean shouldStartGame(Room room) {
        int connectedPlayers = 0;
        for (Participant p : room.getParticipants()) {
            if (p.isConnectedToRoom()) ++connectedPlayers;
        }
        return connectedPlayers >= MIN_PLAYERS;
    }

    // Returns whether the room is in a state where the game should be canceled.
    private boolean shouldCancelGame(Room room) {
        return false;
    }

    @Override
    public void onPeersConnected(Room room, List<String> peers) {
        if (mPlaying) {
            // add new player to an ongoing game
        } else if (shouldStartGame(room)) {
            // start game!
            //TODO main point of entry for game
        }
    }

    @Override
    public void onPeersDisconnected(Room room, List<String> peers) {
        if (mPlaying) {
            // do game-specific handling of this -- remove player's avatar
            // from the screen, etc. If not enough players are left for
            // the game to go on, end the game and leave the room.
        } else if (shouldCancelGame(room)) {
            // cancel the game
            Games.RealTimeMultiplayer.leave(googleApiClient, null, mRoomId);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    public void onPeerLeft(Room room, List<String> peers) {
        // peer left -- see if game should be canceled
        if (!mPlaying && shouldCancelGame(room)) {
            Games.RealTimeMultiplayer.leave(googleApiClient, null, mRoomId);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    public void onPeerDeclined(Room room, List<String> peers) {
        // peer declined invitation -- see if game should be canceled
        if (!mPlaying && shouldCancelGame(room)) {
            Games.RealTimeMultiplayer.leave(googleApiClient, null, mRoomId);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }


    @Override
    public void onJoinedRoom(int statusCode, Room room) {
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            return;
        }

        // get waiting room intent
        Intent i = Games.RealTimeMultiplayer.getWaitingRoomIntent(googleApiClient, room, Integer.MAX_VALUE);
        startActivityForResult(i, REQUEST_WAITING_ROOM);
    }

    @Override
    public void onRoomCreated(int statusCode, Room room) {
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            return;
        }

        // get waiting room intent
        Intent i = Games.RealTimeMultiplayer.getWaitingRoomIntent(googleApiClient, room, Integer.MAX_VALUE);
        startActivityForResult(i, REQUEST_WAITING_ROOM);
    }
    @Override
    public void onDisconnectedFromRoom(Room room) {
        // leave the room
        Games.RealTimeMultiplayer.leave(googleApiClient, null, mRoomId);

        // clear the flag that keeps the screen on
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // show error message and return to main screen
    }


    @Override
    public void onConnected(Bundle connectionHint) {

        if (connectionHint != null) {
            Invitation inv =
                    connectionHint.getParcelable(Multiplayer.EXTRA_INVITATION);

            if (inv != null) {
                // accept invitation
                RoomConfig.Builder roomConfigBuilder = makeBasicRoomConfigBuilder();
                roomConfigBuilder.setInvitationIdToAccept(inv.getInvitationId());
                Games.RealTimeMultiplayer.join(googleApiClient, roomConfigBuilder.build());

                // prevent screen from sleeping during handshake
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                // go to game screen
                startGame();
            }
        }
    }

    @Override
    public void onPeerInvitedToRoom(Room room, List<String> arg1) {
        update(room);
    }

    @Override
    public void onP2PDisconnected(String participant) {
    }

    @Override
    public void onP2PConnected(String participant) {
    }

    @Override
    public void onPeerJoined(Room room, List<String> arg1) {
        update(room);
    }

    @Override
    public void onRoomAutoMatching(Room room) {
        update(room);
    }

    @Override
    public void onRoomConnecting(Room room) {
        update(room);
    }
    @Override
    public void onConnectedToRoom(Room room) {


        // get room ID, participants and my ID:
        mRoomId = room.getRoomId();
        players = room.getParticipants();
        mMyId = room.getParticipantId(Games.Players.getCurrentPlayerId(googleApiClient));

    }
    @Override
    public void onLeftRoom(int statusCode, String roomId) {
    }
    @Override
    public void onInvitationReceived(Invitation invitation) {
    }
    @Override
    public void onInvitationRemoved(String invitationId) {
    }

    @Override
    public void onConnectionSuspended(int i) {
        googleApiClient.connect();//try to reconnect
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onRealTimeMessageReceived(RealTimeMessage rtm) {
        byte[] data = rtm.getMessageData();
        LatLng latLng = new LatLng(ByteBuffer.wrap(data).getDouble(0), ByteBuffer.wrap(data).getDouble(1));
        YTMap.updateMarkers(id, latLng);
    }

    private void sendData(){
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        byte[] data = new byte[16];
        byte[] longitude = doubleToByteArr(location.getLongitude());
        byte[] latitude = doubleToByteArr(location.getLatitude());
        for (int i = 0; i<8;i++){
            data[i] = latitude[i];
        }
        for (int i =8; i<16; i++){
            data[i] = longitude[i];
        }

        for (Participant p : players) {
            if (p.getParticipantId().equals(mMyId)) {
                id = players.indexOf(p);
                continue;
            }
            Games.RealTimeMultiplayer.sendReliableMessage(googleApiClient, null, data,
                        mRoomId, p.getParticipantId());
        }
    }
    private byte[] doubleToByteArr (double d){
        byte[] data = new byte[8];
        ByteBuffer.wrap(data).putDouble(d);
        return data;
    }

    private void startGame(){
        googleApiClient.connect();
        Intent intent = new Intent
                ("com.example.pianonerd77.sandbox.SelectionPage");
        startActivity(intent);
    }

    private void leaveRoom() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (mRoomId != null) {
            Games.RealTimeMultiplayer.leave(googleApiClient, this, mRoomId);
            mRoomId = null;
        } else {
        }
    }

    private void update(Room room) {
        if(room != null){
            players = room.getParticipants();
        }
    }
}




