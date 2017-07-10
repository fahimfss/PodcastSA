package com.example.fahim.podcastsa;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;

public class MainActivity extends AppCompatActivity implements ItemClickListener {

//    private final String URL = "UPIUrl";
    private final String URL = "Url";
    private final String LINK = "link";
    private final String PAGE = "Page";

    private int currentPage;
    protected ArrayList<PodcastItem> podcastItems;

    private RecyclerView podcastRV;
    private ProgressBar loadPodcastRV;
    private PodcastAdapter podcastAdapter;

    private MediaPlayer mediaPlayer;

    private boolean isPaused;
    private boolean isStop ;
    private boolean isOutOfScope;
    private boolean isComplete;

    private boolean isTrackLoading=false;

    private String link = "";

    private ImageButton pageGOBtn;

    private ImageButton musicPlayBtn;
    private TextView musicTimeTV;
    private TextView musicTotalTimeTV;
    private TextView musicTitle;
    private SeekBar musicSeekbar;

    private EditText pageET;

    private boolean pageLoading;

    private int selectedPodcast=-1;

    private int updateEverySecondCount;

    private int initialLoading=0;

    private boolean itemClickLock=false;

    private HashSet<String> firebaseDatas;

    FirebaseAuth auth;
    FirebaseDatabase database;
    DatabaseReference reference;

    Query query1;
    ValueEventListener listener1;
    Query query2;
    ValueEventListener listener2;
    Query query3;
    ValueEventListener listener3;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseApp.initializeApp(this);

        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            auth.signInWithEmailAndPassword("fahim.cross@gmail.com", "121212");
        }

        database = FirebaseDatabase.getInstance();
        reference = database.getReference();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getInt("page", -1) != -1) {
            currentPage = prefs.getInt("page", -1);
        } else {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("page", 1);
            currentPage = 1;
            editor.apply();
        }

        isPaused = false;
        isStop = true;
        isOutOfScope = false;
        isComplete = false;

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        podcastRV = (RecyclerView) findViewById(R.id.podcastRV);
        loadPodcastRV = (ProgressBar) findViewById(R.id.loadPodcastRV);

        LinearLayoutManager llm = new LinearLayoutManager(this);
        podcastRV.setLayoutManager(llm);

        setTitle("Page: " + String.valueOf(currentPage));

        podcastRV.setVisibility(View.GONE);
        loadPodcastRV.setVisibility(View.VISIBLE);

        musicPlayBtn = (ImageButton) findViewById(R.id.musicPlayBtn);
        musicTimeTV = (TextView) findViewById(R.id.musicTimeTV);
        musicTotalTimeTV = (TextView) findViewById(R.id.musicTotalTimeTV);
        musicSeekbar = (SeekBar) findViewById(R.id.musicSeekbar);
        musicTitle = (TextView) findViewById(R.id.musicTitle);

        updateEverySecondCount = 0;
        pageLoading = true;

        pageET = (EditText)findViewById(R.id.pageNo);
        pageGOBtn = (ImageButton) findViewById(R.id.pageGoBtn);
        pageET.setImeOptions(EditorInfo.IME_ACTION_DONE);
        pageET.setSingleLine();
        pageET.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    hideKeyboard(MainActivity.this);
                    pageGOBtn.performClick();
                    return true;
                }
                return false;
            }
        });

        fetchFromFirebase();

        setPlayer();

        new Data().execute(String.valueOf(currentPage));
    }

    @Override
    protected void onResume() {
        super.onResume();
        isOutOfScope = false;
        updateEverySecond();
        if(isComplete && !isStop){
            musicSeekbar.setProgress(0);
            musicTimeTV.setText(String.format("%02d:%02d", 0, 0));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isOutOfScope = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(listener1 !=null)query1.removeEventListener(listener1);
        if(listener2 !=null)query2.removeEventListener(listener2);
        if(listener3 !=null)query3.removeEventListener(listener3);
        mediaPlayer.release();
    }

    private void dataFetched() {
        pageLoading = false;
        podcastRV.setVisibility(View.VISIBLE);
        loadPodcastRV.setVisibility(View.GONE);
    }

    @Override
    public void itemClick(int position, boolean turnHighlightOff) throws InterruptedException {
        if(itemClickLock)return;
        itemClickLock = true;

        FirebaseData firebaseData = new FirebaseData(podcastItems.get(position).getLink());

        if(podcastAdapter != null) {

            if (!firebaseDatas.contains(firebaseData.getLink())) {
                firebaseDatas.add(firebaseData.getLink());
                insertToFirebase(firebaseData);
            }

            if (turnHighlightOff) {
                if (firebaseDatas.contains(firebaseData.getLink())) {
                    firebaseDatas.remove(firebaseData.getLink());
                    deleteFromFirebase(firebaseData);
                    podcastItems.get(position).setStatus(0);
                    podcastAdapter.notifyDataSetChanged();
                }
            }
            else {
                podcastItems.get(position).setPlaying(1);
                podcastItems.get(position).setStatus(1);

                if(selectedPodcast != position) {
                    if(selectedPodcast!=-1)podcastItems.get(selectedPodcast).setPlaying(0);
                    selectedPodcast = position;

                    isPaused = false;
                    isStop = false;

                    link = "http://www.scientificamerican.com" + firebaseData.getLink();

                    musicPlayBtn.setImageResource(R.drawable.ic_pause);
                    musicPlayBtn.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    resetAndStartPlayer();
                }
                podcastAdapter.notifyDataSetChanged();
            }
        }
        itemClickLock = false;
    }

    public void musicPlayBtnClicked(View v) {
        if (selectedPodcast == -1 || isStop || isTrackLoading) return;
        if (isPaused) {
            mediaPlayer.start();
            isPaused = false;
            updateEverySecond();
            musicPlayBtn.setImageResource(R.drawable.ic_pause);
            musicPlayBtn.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        } else {
            mediaPlayer.pause();
            isPaused = true;
            musicPlayBtn.setImageResource(R.drawable.ic_play);
            musicPlayBtn.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        }
    }

    private void updateEverySecond() {
        Thread t = new Thread() {
            Handler handler = new Handler();

            @Override
            public void run() {
                if (mediaPlayer != null && updateEverySecondCount == 0) {
                    updateEverySecondCount++;
                    while ((!isPaused || !isComplete) && !isStop) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if(!isOutOfScope)setProgressTimeAndSeek();
                            }
                        });
                        try {
                            Thread.sleep(250);
                        } catch (InterruptedException e) {
                            return;
                        } catch (Exception e) {
                            return;
                        }
                    }
                    updateEverySecondCount--;
                }
            }
        };
        t.start();
    }

    protected void setProgressTimeAndSeek() {
        if(isStop || isTrackLoading || mediaPlayer==null)return;
        int curPosition = mediaPlayer.getCurrentPosition();
        musicSeekbar.setProgress(curPosition);
        musicTimeTV.setText(String.format("%02d:%02d", curPosition / 60000, (curPosition / 1000) % 60));
    }

    private void setPlayer() {

        getWindow().setFormat(PixelFormat.UNKNOWN);
        mediaPlayer = new MediaPlayer();

        mediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {

            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                musicSeekbar.setSecondaryProgress((musicSeekbar.getMax() / 100) * percent);
                if(percent>15 && mediaPlayer!=null && !isPaused){
                    Log.d("On BUffering Update", "here");
                    isTrackLoading = false;
                    mediaPlayer.start();
                    updateEverySecond();
                }
            }
        });

        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mediaPlayer.start();
                MainActivity.this.setProgressControl();
            }
        });

        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                mediaPlayer.release();
                mediaPlayer = null;
                if(!isStop) {
                    Toast.makeText(MainActivity.this, "Can not play this "+what, Toast.LENGTH_SHORT).show();
                }
                isStop = true;
                resetMedia();
                return false;
            }
        });
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if(selectedPodcast == podcastItems.size()-1) {
                    isPaused = true;
                    isComplete = true;
                    mediaPlayer.seekTo(0);
                    musicSeekbar.setProgress(0);
                    musicTimeTV.setText(String.format("%02d:%02d", 0, 0));
                    musicPlayBtn.setImageResource(R.drawable.ic_play);
                    musicPlayBtn.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                }
                else {
                    try {
                        musicSeekbar.setProgress(0);
                        musicTimeTV.setText(String.format("%02d:%02d", 0, 0));
                        isComplete = true;
                        itemClick(selectedPodcast+1, false);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void setProgressControl() {
        int maxProgress = mediaPlayer.getDuration();
        int curProgress = mediaPlayer.getCurrentPosition();
        mediaPlayer.pause();

        musicSeekbar.setMax(maxProgress);
        musicSeekbar.setProgress(curProgress);
        musicSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekbar, int progress, boolean fromUser) {
                if (fromUser && !isTrackLoading) {
                    mediaPlayer.seekTo(progress);
                    setProgressTimeAndSeek();
                    if(isPaused)mediaPlayer.pause();
                    else mediaPlayer.start();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub

            }
        });
        musicTitle.setText(podcastItems.get(selectedPodcast).getTitle());
        musicTotalTimeTV.setText(podcastItems.get(selectedPodcast).getLenght());
    }

    private void resetAndStartPlayer() {
        try {
            isTrackLoading=true;

            musicTitle.setText("Loading...");
            musicTimeTV.setText(String.format("%02d:%02d", 0, 0));
            musicTotalTimeTV.setText(String.format("%02d:%02d", 0, 0));

            musicSeekbar.setProgress(0);
            musicSeekbar.setSecondaryProgress(0);

            if (link != null) {
                if(mediaPlayer == null) setPlayer();
                else {
                    mediaPlayer.release();
                    mediaPlayer = null;
                    setPlayer();
                }
                mediaPlayer.setDataSource(link);
                mediaPlayer.prepareAsync();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void GO(View v){
        String sPage = ((EditText)findViewById(R.id.pageNo)).getText().toString();
        int page;
        try{
            page = Integer.valueOf(sPage);
        }catch (Exception e){
            return;
        }
        selectedPodcast = -1;
        isStop = true;
        isPaused = true;
        if(mediaPlayer!=null && mediaPlayer.isPlaying())mediaPlayer.reset();
        if(pageLoading)return;
        pageLoading = true;
        currentPage = page;
        ((EditText)findViewById(R.id.pageNo)).setText("");
        podcastRV.setVisibility(View.GONE);
        loadPodcastRV.setVisibility(View.VISIBLE);

        resetMedia();

        new Data().execute(String.valueOf(currentPage));
    }

    public void resetMedia(){
        musicTitle.setText("---");
        musicSeekbar.setSecondaryProgress(0);
        musicSeekbar.setProgress(0);
        musicTimeTV.setText("00:00");
        musicTotalTimeTV.setText("00:00");
        musicPlayBtn.setImageResource(R.drawable.ic_play);
        musicPlayBtn.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        setTitle("Page: " + String.valueOf(currentPage));
        link = null;
        hideKeyboard(MainActivity.this);
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = activity.getCurrentFocus();
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.podcast_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        selectedPodcast = -1;
        switch (item.getItemId()) {
            case R.id.prevPage:
                isStop = true;
                isPaused = true;
                if(mediaPlayer!=null && mediaPlayer.isPlaying())mediaPlayer.reset();
                if(pageLoading)return true;
                pageLoading = true;
                if (currentPage > 1) {
                    currentPage--;

                    podcastRV.setVisibility(View.GONE);
                    loadPodcastRV.setVisibility(View.VISIBLE);

                    resetMedia();

                    new Data().execute(String.valueOf(currentPage));
                }
                return true;
            case R.id.nextPage:
                isStop = true;
                isPaused = true;
                if(mediaPlayer!=null && mediaPlayer.isPlaying())mediaPlayer.reset();
                if(pageLoading)return true;
                currentPage++;
                pageLoading = true;

                podcastRV.setVisibility(View.GONE);
                loadPodcastRV.setVisibility(View.VISIBLE);

                resetMedia();

                new Data().execute(String.valueOf(currentPage));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void fetchFromFirebase(){
        query1 = reference.child(URL).orderByChild(LINK);
        listener1 = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                firebaseDatas = new HashSet<>();
                for(DataSnapshot ds: dataSnapshot.getChildren()){
                    FirebaseData data = ds.getValue(FirebaseData.class);
                    firebaseDatas.add(data.getLink());
                }
                if(podcastItems!=null){
                    for(PodcastItem item: podcastItems){
                        if(firebaseDatas.contains(item.getLink())){
                            item.setStatus(true);
                        }
                    }
                    podcastAdapter.notifyDataSetChanged();
                }
                initialLoading++;
                if(initialLoading==2){
                    initialLoading = -1;
                    dataFetched();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        query1.addListenerForSingleValueEvent(listener1);
    }

    private void insertToFirebase(final FirebaseData data){
        query2 = reference.child(URL).orderByChild(LINK).equalTo(data.getLink());
        listener2 = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(!dataSnapshot.hasChildren()) {
                    reference.child(URL).push().setValue(data);
                }
                podcastAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        query2.addListenerForSingleValueEvent(listener2);
    }

    private void deleteFromFirebase(final FirebaseData data){
        query3 = reference.child(URL).orderByChild(LINK).equalTo(data.getLink());
        listener3 = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot ds: dataSnapshot.getChildren()) {
                    reference.child(URL).child(ds.getKey()).setValue(null);
                }
                podcastAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        query3.addListenerForSingleValueEvent(listener3);
    }

    private class Data extends DataSource {
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("page", currentPage);
            editor.apply();

            podcastItems = podcastItemsGET;
            podcastAdapter = new PodcastAdapter(podcastItems, MainActivity.this, MainActivity.this);
            podcastRV.setAdapter(podcastAdapter);
            if(firebaseDatas!=null) {
                for (PodcastItem pi : podcastItems) {
                    pi.setStatus(firebaseDatas.contains(pi.getLink()));
                }
                if (listener1 != null) query1.removeEventListener(listener1);
            }
            if(initialLoading!=-1){
                initialLoading++;
                if(initialLoading==2){
                    initialLoading = -1;
                    dataFetched();
                }
            }
            else{
                dataFetched();
            }
        }
    }

}

/*
* isTrackLoading: Initial loading period for a track. Without this media player shutters
* and display odd behaviour.
* starts: At mediaplayer prepareasync
* ends: OnBufferUpdate percent > 15
* Task: media player ui controls wont work
*
* seekbar seekToZero:
* Occurs: 1. ItemClick: diff pos, next pos.
* 2. isStop
* 3. isTrackLoading
*
* seekTo():
* wont work when: 1. isStop
* 2. isTrackLoading
*
* updateEverySecond:
* start: OnBufferUpdate percent > 15
*
* */

