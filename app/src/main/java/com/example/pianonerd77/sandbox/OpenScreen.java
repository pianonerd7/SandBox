package com.example.pianonerd77.sandbox;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.OnInvitationReceivedListener;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateListener;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;
import com.google.android.gms.plus.Plus;

import java.util.ArrayList;
import java.util.List;

public class OpenScreen extends ActionBarActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, RoomStatusUpdateListener, RoomUpdateListener,
        OnInvitationReceivedListener,RealTimeMessageReceivedListener
{

    private final static int REQUEST_SELECT_PLAYERS = 1000;

    final static int REQUEST_WAITING_ROOM = 1002;

    private GoogleApiClient googleApiClient;

    // are we already playing?
    boolean mPlaying = false;

    // at least 2 players required for our game
    final static int MIN_PLAYERS = 2;


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
        if (request == REQUEST_SELECT_PLAYERS){
            if (response != Activity.RESULT_OK){
                return; //response is either canceled or first, either of which are not okay
            }
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
            // display error
            return;
        }

        // get waiting room intent
        Intent i = Games.RealTimeMultiplayer.getWaitingRoomIntent(googleApiClient, room, Integer.MAX_VALUE);
        startActivityForResult(i, REQUEST_WAITING_ROOM);
    }

    @Override
    public void onRoomCreated(int statusCode, Room room) {
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            // display error
            return;
        }

        // get waiting room intent
        Intent i = Games.RealTimeMultiplayer.getWaitingRoomIntent(googleApiClient, room, Integer.MAX_VALUE);
        startActivityForResult(i, REQUEST_WAITING_ROOM);
    }

    @Override
    public void onActivityResult(int request, int response, Intent intent) {
        if (request == REQUEST_WAITING_ROOM) {
            if (response == Activity.RESULT_OK) {
                // (start game)
            }
            else if (response == Activity.RESULT_CANCELED) {
                // Waiting room was dismissed with the back button. The meaning of this
                // action is up to the game. You may choose to leave the room and cancel the
                // match, or do something else like minimize the waiting room and
                // continue to connect in the background.

                // in this example, we take the simple approach and just leave the room:
                Games.RealTimeMultiplayer.leave(googleApiClient, null, mRoomId);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
            else if (response == GamesActivityResultCodes.RESULT_LEFT_ROOM) {
                // player wants to leave the room.
                Games.RealTimeMultiplayer.leave(googleApiClient, null, mRoomId);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }
    }

final static int MIN_PLAYERS = 2;

// get waiting room intent
Intent i = Games.RealTimeMultiplayer.getWaitingRoomIntent(googleApiClient, room, Integer.MIN_PLAYERS);
    startActivityForResult(i, RC_WAITING_ROOM);

boolean mWaitingRoomFinishedFromCode = false;

// if "start game" message is received:
mWaitingRoomFinishedFromCode = true;
        finishActivity(RC_WAITING_ROOM);

@Override
public void onActivityResult(int request, int response, Intent intent) {
        if (request == REQUEST_WAITING_ROOM) {
        // ignore response code if the waiting room was dismissed from code:
        if (mWaitingRoomFinishedFromCode) return;

        // ...(normal implementation, as above)...
        }
        }

        Set<String> mFinishedRacers;

        boolean haveAllRacersFinished(Room room) {
        for (Participant p : room.getParticipants()) {
        String pid = p.getParticipantId();
        if (p.isConnectedToRoom() && !mFinishedRacers.contains(pid)) {
        // at least one racer is connected but hasn't finished
        return false;
        }
        }
        // all racers who are connected have finished the race
        return true;
        }

@Override
public void onDisconnectedFromRoom(Room room) {
        // leave the room
        Games.RealTimeMultiplayer.leave(mGoogleApiClient, null, mRoomId);

        // clear the flag that keeps the screen on
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // show error message and return to main screen
        }

private RoomConfig.Builder makeBasicRoomConfigBuilder() {
        return RoomConfig.builder(this)
        .setMessageReceivedListener(this)
        .setRoomStatusUpdateListener(this)
        }

@Override
public void onConnected(Bundle connectionHint) {
        // ...

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
        }
        }
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

@Override
protected void onConnected(Bundle connectionHint) {
        // ...
        Games.Invitations.registerInvitationListener(mGoogleApiClient, mListener);
        // ...
        }

@Override
public void onInvitationReceived(Invitation invitation) {
        // show in-game popup to let user know of pending invitation

        // store invitation for use when player accepts this invitation
        mIncomingInvitationId = invitation.getInvitationId();
        }

        RoomConfig.Builder roomConfigBuilder = makeBasicRoomConfigBuilder();
        roomConfigBuilder.setInvitationIdToAccept(mIncomingInvitationId);
        Games.RealTimeMultiplayer.join(mGoogleApiClient, roomConfigBuilder.build());

// prevent screen from sleeping during handshake
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

// now, go to game screen

// request code (can be any number, as long as it's unique)
final static int RC_INVITATION_INBOX = 10001;

// launch the intent to show the invitation inbox screen
        Intent intent = Games.Invitations.getInvitationInboxIntent();
        mActivity.startActivityForResult(intent, RC_INVITATION_INBOX);

@Override
public void onActivityResult(int request, int response, Intent data) {
        if (request == RC_INVITATION_INBOX) {
        if (response != Activity.RESULT_OK) {
        // canceled
        return;
        }

        // get the selected invitation
        Bundle extras = data.getExtras();
        Invitation invitation =
        extras.getParcelable(Multiplayer.EXTRA_INVITATION);

        // accept it!
        RoomConfig roomConfig = makeBasicRoomConfigBuilder()
        .setInvitationIdToAccept(invitation.getInvitationId())
        .build();
        Games.RealTimeMultiplayer.join(mGoogleApiClient, roomConfig);

        // prevent screen from sleeping during handshake
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // go to game screen
        }
        }
}




