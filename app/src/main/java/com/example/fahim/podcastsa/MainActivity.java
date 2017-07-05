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
import android.support.v7.widget.CardView;
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

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements ItemClickListener {

    private DatabaseHelper helper;
    private int currentPage;
    protected ArrayList<PodcastItem> podcastItems;

    private RecyclerView podcastRV;
    private ProgressBar loadPodcastRV;

    private MediaPlayer mediaPlayer;

    private boolean isPaused;
    private boolean isStop ;
    private boolean isOutOfScope;
    private boolean isComplete;

    private String link = "";

    private ImageButton pageGOBtn;

    private ImageButton musicPlayBtn;
    private TextView musicTimeTV;
    private TextView musicTotalTimeTV;
    private TextView musicTitle;
    private SeekBar musicSeekbar;

    private EditText pageET;

    private boolean pageLoading;

    private int selectedPodcast;
    private int updateEverySecondCount;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        helper = new DatabaseHelper(this);

        podcastRV = (RecyclerView) findViewById(R.id.podcastRV);
        loadPodcastRV = (ProgressBar) findViewById(R.id.loadPodcastRV);

        podcastRV.setHasFixedSize(true);
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

        selectedPodcast = -1;
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
        helper.close();
        mediaPlayer.release();
    }

    private void dataFetched() {
        for (PodcastItem p : podcastItems) {
            p.setStatus(helper.isLinkExist(p.getLink(), helper));
        }
        podcastRV.setAdapter(new PodcastAdapter(podcastItems, this, this));
        podcastRV.setVisibility(View.VISIBLE);
        loadPodcastRV.setVisibility(View.GONE);
    }

    @Override
    public void itemClick(int position) throws InterruptedException {
        selectedPodcast = position;

        isPaused = false;
        isComplete = false;
        isStop = false;

        helper.putLink(podcastItems.get(position).getLink(), helper);
        musicTitle.setText(podcastItems.get(position).getTitle());
        musicTotalTimeTV.setText(podcastItems.get(position).getLenght());
        link = "http://www.scientificamerican.com" + podcastItems.get(position).getLink();

        musicPlayBtn.setImageResource(R.drawable.ic_pause);
        musicPlayBtn.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        resetAndStartPlayer();
    }

    public void musicPlayBtnClicked(View v) {
        if (selectedPodcast == -1 || isStop) return;
        if (isComplete) {
            mediaPlayer.start();

            isPaused = false;
            isComplete = false;

            updateEverySecond();
            musicPlayBtn.setImageResource(R.drawable.ic_pause);
            musicPlayBtn.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            return;
        }
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
                    while ((!isPaused || !isComplete) && !isStop && !isOutOfScope) {
                        musicSeekbar.setProgress(mediaPlayer.getCurrentPosition());
                        handler.post(new Runnable() {

                            @Override
                            public void run() {
                                setProgressText();
                            }
                        });
                        try {
                            Thread.sleep(125);
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

    protected void setProgressText() {
        if(isStop)return;
        int curPosition = mediaPlayer.getCurrentPosition();
        musicTimeTV.setText(String.format("%02d:%02d", curPosition / 60000, (curPosition / 1000) % 60));
    }

    private void setPlayer() {

        getWindow().setFormat(PixelFormat.UNKNOWN);
        mediaPlayer = new MediaPlayer();

        mediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {

            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                musicSeekbar.setSecondaryProgress((musicSeekbar.getMax() / 100) * percent);
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
                    Toast.makeText(MainActivity.this, "Can not play this", Toast.LENGTH_SHORT).show();
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
                isPaused = true;
                isComplete = true;
                mediaPlayer.seekTo(0);
                musicSeekbar.setProgress(0);
                musicTimeTV.setText(String.format("%02d:%02d", 0, 0));
                musicPlayBtn.setImageResource(R.drawable.ic_play);
                musicPlayBtn.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            }
        });
    }

    private void setProgressControl() {
        int maxProgress = mediaPlayer.getDuration();
        int curProgress = mediaPlayer.getCurrentPosition();

        musicSeekbar.setMax(maxProgress);
        musicSeekbar.setProgress(curProgress);
        musicSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekbar, int progress, boolean fromUser) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress);
                    setProgressText();
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
    }

    private void resetAndStartPlayer() {
        try {
            if (link != null) {
                if(mediaPlayer == null) setPlayer();
                mediaPlayer.reset();
                mediaPlayer.setDataSource(link);
                mediaPlayer.prepareAsync();
                updateEverySecond();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void GO(View v){
        isStop = true;
        isPaused = true;
        if(mediaPlayer!=null && mediaPlayer.isPlaying())mediaPlayer.reset();
        if(pageLoading)return;
        pageLoading = true;
        currentPage = Integer.valueOf(((EditText)findViewById(R.id.pageNo)).getText().toString());
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

    private class Data extends DataSource {
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("page", currentPage);
            editor.apply();

            podcastItems = podcastItemsGET;
            Log.d("Datafetched", String.valueOf(podcastItems.size()));
            for (PodcastItem pi : podcastItems) {
                pi.setStatus(helper.isLinkExist(pi.getLink(), helper));
            }
            pageLoading = false;
            dataFetched();
        }
    }
}
