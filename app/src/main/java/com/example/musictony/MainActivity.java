package com.example.musictony;

import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.PackageManagerCompat;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.chibde.visualizer.BarVisualizer;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerLibraryInfo;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.jgabrielfreitas.core.BlurImageView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {


    RecyclerView recyclerView;
    SongAdapter songAdapter;
    List<Song> allSongs = new ArrayList<>();
    ActivityResultLauncher<String> storagePermissionLauncher;
    final String permission = Manifest.permission.READ_MEDIA_AUDIO;

    ExoPlayer player;
    ActivityResultLauncher<String> recordAudioPermissionLauncher;
    final String recordAudioPermission = Manifest.permission.RECORD_AUDIO;
    ConstraintLayout playerView;
    TextView playerCloseBtn;
    TextView songNameView, skipPreviousBtn, skipNextBtn, repeatModeBtn, playlistBtn, playPauseBtn;
    TextView homeSongNameView, homeSkipPreviousBtn, homePlayPauseBtn, homeSkipNextBtn;

    ConstraintLayout homeControlWrapper, headWrapper, artworkWrapper, seekbarWrapper, controlWrapper, audioVisualizerWrapper;
    CircleImageView artworkView;
    SeekBar seekbar;
    TextView progressView, durationView;

    BarVisualizer audioVisualizer;

    BlurImageView blurImageView;
    int defaultStatusColor;
    int repeatMode = 1; // repeat all 1, repeat one 2, shuffle all 3
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        defaultStatusColor = getWindow().getStatusBarColor();

        getWindow().setNavigationBarColor(ColorUtils.setAlphaComponent(defaultStatusColor, 199));

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle(getResources().getString(R.string.app_name));

        recyclerView = findViewById(R.id.recyclerview);

        storagePermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) {
                fetchSongs();
            } else {
                userResponses();
            }
        });
        storagePermissionLauncher.launch(permission);
        recordAudioPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted->{
            if(granted && player.isPlaying()) {
                activateAudioVisualizer();
            }
            else {
                userResponsesOnRecordAudioPerm();
            }
        });
        //Views
        player = new ExoPlayer.Builder(this).build();
        playerView = findViewById(R.id.playerView);
        playerCloseBtn = findViewById(R.id.playerCloseBtn);
        songNameView = findViewById(R.id.songNameView);
        skipPreviousBtn = findViewById(R.id.skipPreviewBtn);
        skipNextBtn = findViewById(R.id.skipNextBtn);
        playPauseBtn = findViewById(R.id.playPauseBtn);
        repeatModeBtn = findViewById(R.id.repeatModeBtn);
        playlistBtn = findViewById(R.id.playlistBtn);

        homeSongNameView = findViewById(R.id.homeSongNameView);
        homeSkipPreviousBtn = findViewById(R.id.homeSkipPreviousBtn);
        homeSkipNextBtn = findViewById(R.id.homeSkipNextBtn);
        homePlayPauseBtn = findViewById(R.id.homePlayPauseBtn);

        homeControlWrapper = findViewById(R.id.homeControlWrapper);
        headWrapper = findViewById(R.id.headWrapper);
        artworkWrapper = findViewById(R.id.artworkWrapper);
        seekbarWrapper = findViewById(R.id.seekbarWrapper);
        controlWrapper = findViewById(R.id.controlWrapper);
        audioVisualizerWrapper = findViewById(R.id.audioVisualizerWrapper);

        artworkView = findViewById(R.id.artworkView);
        seekbar = findViewById(R.id.seekbar);
        progressView = findViewById(R.id.progressView);
        durationView = findViewById(R.id.durationView);
        audioVisualizer = findViewById(R.id.visualizer);
        
        blurImageView = findViewById(R.id.blurImageView);
        
        playerControls();


    }

    private void playerControls() {
        songNameView.setSelected(true);
        homeSongNameView.setSelected(true);

        playerCloseBtn.setOnClickListener(view -> exitPlayerView());
        playlistBtn.setOnClickListener(view -> exitPlayerView());
        homeControlWrapper.setOnClickListener(view -> showPlayerView());

        player.addListener(new Player.Listener() {
            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                Player.Listener.super.onMediaItemTransition(mediaItem, reason);
                assert mediaItem != null;
                songNameView.setText(mediaItem.mediaMetadata.title);
                homeSongNameView.setText(mediaItem.mediaMetadata.title);

                progressView.setText(getReadableTime((int) player.getCurrentPosition()));
                seekbar.setProgress((int) player.getCurrentPosition());
                seekbar.setMax((int)player.getDuration());
                durationView.setText(getReadableTime((int) player.getDuration()));
                playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause_outline,0,0,0);
                homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause,0,0,0);


                showCurrentArtwork();
                updatePlayerPositionProgress();
                //load the artwork animation
                artworkView.setAnimation(loadRotation());

                activateAudioVisualizer();

                updatePlayerColors();

                if(!player.isPlaying()){
                    player.play();
                }

            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                Player.Listener.super.onPlaybackStateChanged(playbackState);
                if(playbackState == ExoPlayer.STATE_READY){
                    songNameView.setText(Objects.requireNonNull(player.getCurrentMediaItem().mediaMetadata.title));
                    homeSongNameView.setText(player.getCurrentMediaItem().mediaMetadata.title);
                    progressView.setText(getReadableTime((int) player.getCurrentPosition()));
                    durationView.setText(getReadableTime((int) player.getDuration()));
                    seekbar.setMax((int) player.getDuration());
                    seekbar.setProgress((int) player.getCurrentPosition());
                    playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause_outline,0,0,0);
                    homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause,0,0,0);
                    showCurrentArtwork();
                    updatePlayerPositionProgress();
                    //load the artwork animation
                    artworkView.setAnimation(loadRotation());
                    activateAudioVisualizer();
                    updatePlayerColors();
                }
                else {
                    playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_outline,0,0,0);
                    homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play,0,0,0);
                }
            }
        });

        //skip to next trac
        skipNextBtn.setOnClickListener(view -> skipToNextSong());
        homeSkipNextBtn.setOnClickListener(view -> skipToNextSong());

        //skip to previous track
        skipPreviousBtn.setOnClickListener(view -> skipToPreviousSong());
        homeSkipPreviousBtn.setOnClickListener(view -> skipToPreviousSong());

        //play or pause the player
        playPauseBtn.setOnClickListener(view -> playOrPausePlayer());
        homePlayPauseBtn.setOnClickListener(view -> playOrPausePlayer());

        //seekbar listener
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressValue =0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                progressValue = seekBar.getProgress();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(player.getPlaybackState() == ExoPlayer.STATE_READY){
                    seekBar.setProgress(progressValue);
                    progressView.setText(getReadableTime(progressValue));
                    player.seekTo(progressValue);
                }

            }
        });
        //repeat mode
        repeatModeBtn.setOnClickListener(view -> {
            if(repeatMode==1) {
                player.setRepeatMode(ExoPlayer.REPEAT_MODE_ONE);
                repeatMode = 2;
                repeatModeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_repeat_one,0,0,0);
            }
            else if(repeatMode == 3){
                player.setRepeatMode(ExoPlayer.REPEAT_MODE_ALL);
                player.setShuffleModeEnabled(false);
                repeatMode = 1;
                repeatModeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_repeat_all,0,0,0);
            }
            updatePlayerColors();
        });

    }

    private void playOrPausePlayer() {
        if(player.isPlaying()){
            player.pause();
            playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_outline,0,0,0);
            homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play,0,0,0);
            artworkView.clearAnimation();
        }
        else {
            player.play();
            playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause_outline,0,0,0);
            homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause,0,0,0);
            artworkView.startAnimation(loadRotation());
        }
        updatePlayerColors();
    }

    private void skipToNextSong() {
        if(player.hasNextMediaItem()){
            player.seekToNext();
        }

    }

    private void skipToPreviousSong() {
        if(player.hasPreviousMediaItem()){
            player.seekToPrevious();
        }
    }

    private Animation loadRotation() {
        RotateAnimation rotateAnimation = new RotateAnimation(0,360, Animation.RELATIVE_TO_SELF,0.5f,Animation.RELATIVE_TO_SELF,0.5f);
        rotateAnimation.setInterpolator(new LinearInterpolator());
        rotateAnimation.setDuration(10000);
        rotateAnimation.setRepeatCount(Animation.INFINITE);
        return rotateAnimation;
    }

    private void updatePlayerPositionProgress() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(player.isPlaying()) {
                    progressView.setText(getReadableTime((int) player.getCurrentPosition()));
                    seekbar.setProgress((int)player.getCurrentPosition());
                }
                updatePlayerPositionProgress();
            }
        },1000);
    }

    private void showCurrentArtwork() {
        artworkView.setImageURI(Objects.requireNonNull(player.getCurrentMediaItem()).mediaMetadata.artworkUri);

        if(artworkView.getDrawable() == null) {
            artworkView.setImageResource(R.drawable.default_artwork);
        }
    }

    private void showPlayerView() {
        playerView.setVisibility(View.VISIBLE);
        updatePlayerColors();
        
    }

    private void updatePlayerColors() {
    }

    private void exitPlayerView() {
        playerView.setVisibility(View.GONE);
        getWindow().setStatusBarColor(defaultStatusColor);
        getWindow().setNavigationBarColor(ColorUtils.setAlphaComponent(defaultStatusColor,199));
    }

    private void userResponsesOnRecordAudioPerm() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(shouldShowRequestPermissionRationale(recordAudioPermission)) {
                //show an UI explaining why need this permission
                //alert dialog
                new AlertDialog.Builder(this)
                        .setTitle("Requesting to show AudioVisualizer")
                        .setMessage("Allow this app to display audio visualizer when music is active na")
                        .setPositiveButton("allow", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //request the perm
                                recordAudioPermissionLauncher.launch(recordAudioPermission);
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Toast.makeText(getApplicationContext(), "You denied to show the audio visualizer", Toast.LENGTH_SHORT).show();
                                dialogInterface.dismiss();
                            }
                        })
                        .show();
            }
            else {
                Toast.makeText(getApplicationContext(), "You denied to show the audio visualizer", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void activateAudioVisualizer() {
    }

    private void fetchSongs() {
        List<Song> songs = new ArrayList<>();
        Uri mediaStoreUri;

        mediaStoreUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String[] projection = new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.ALBUM_ID,
        };

        String sortOrder = MediaStore.Audio.Media.DATE_ADDED + " DESC";

        //get the songs
        try(Cursor cursor = getContentResolver().query(mediaStoreUri, projection, null, null, sortOrder)) {
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
            int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
            int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);
            int albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);

            while(cursor.moveToNext()){
                //get the values of column for a give audio file
                long id = cursor.getLong(idColumn);
                String name = cursor.getString(nameColumn);
                int duration = cursor.getInt(durationColumn);
                int size = cursor.getInt(sizeColumn);
                long albumId = cursor.getLong(albumIdColumn);



                //media uri
                Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);

                //album art
                Uri albumArtworkUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId);

                name = name.substring(0,name.lastIndexOf("."));

                Song song = new Song(name,uri, albumArtworkUri, size, duration);

                songs.add(song);
            }

        showSongs(songs);

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //release player
        if(player.isPlaying()) {
            player.stop();
        }
        player.release();
    }
    private String getReadableTime(int totalDuration) {
        String totalDurationText;
        int hrs = totalDuration/(1000*60*60);
        int min = (totalDuration%(1000*60*60))/(1000*60);
        int secs = (((totalDuration%(1000*60*60))%(1000*60*60))%(1000*60))/1000;

        if(hrs<1){
            totalDurationText = String.format("%02d:%02d",min, secs);
        }
        else {
            totalDurationText = String.format("%1d:%02d:%02d",hrs,min,secs);
        }
        return totalDurationText;
    }
    private void showSongs(List<Song> songs) {
        if(songs.size() == 0) {
            Toast.makeText(this,"No Songs",Toast.LENGTH_SHORT).show();
            return;
        }

        allSongs.clear();
        allSongs.addAll(songs);

        //tool bar title
        String title = getResources().getString(R.string.app_name) + "-" + songs.size();
        Objects.requireNonNull(getSupportActionBar()).setTitle(title);

        //layout manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);



        songAdapter = new SongAdapter(this,songs,player,playerView);
//        songAdapter.setHasStableIds(true);
        recyclerView.setAdapter(songAdapter);

        //animation


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_btn, menu);

        MenuItem  menuItem = menu.findItem(R.id.searchBtn);
        SearchView searchView = (SearchView) menuItem.getActionView();

        SearchSong(searchView);
        return super.onCreateOptionsMenu(menu);
    }
    private void SearchSong(SearchView searchView) {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterSongs(newText.toLowerCase());
                return true;
            }
        });
    }
    private void filterSongs(String query){
        List<Song> filteredList = new ArrayList<>();

        if(allSongs.size()>0){
            for(Song song : allSongs){
                if(song.getTitle().toLowerCase().contains(query)){
                    filteredList.add(song);
                }
            }
            if(songAdapter != null ) {
                songAdapter.filterSongs(filteredList);
            }
        }
    }
    private void userResponses() {
        if(ContextCompat.checkSelfPermission(this,permission) == PackageManager.PERMISSION_GRANTED) {
            fetchSongs();
        }
        else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(shouldShowRequestPermissionRationale(permission)) {
                new AlertDialog.Builder(this)
                        .setTitle("Requesting Permission")
                        .setMessage("Allow me to fetch songs on the device")
                        .setPositiveButton("allow", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                storagePermissionLauncher.launch(permission);
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Toast.makeText(getApplicationContext(),"You denied me to show songs", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .show();
            }
        }
        else {
            Toast.makeText(this,"You canceled to show songs", Toast.LENGTH_SHORT).show();
        }
    }
}