package com.editor.videotrimmer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.ExecuteCallback;
import com.arthenica.mobileffmpeg.FFmpeg;
import com.arthenica.mobileffmpeg.LogCallback;
import com.arthenica.mobileffmpeg.LogMessage;
import com.arthenica.mobileffmpeg.Statistics;
import com.arthenica.mobileffmpeg.StatisticsCallback;

import org.florescu.android.rangeseekbar.RangeSeekBar;

import java.io.File;
import java.util.Arrays;

import static android.content.ContentValues.TAG;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;
import static java.lang.String.format;

public class TrimActivity extends AppCompatActivity {

    Uri uri;
    ImageView imageView;
    VideoView videoView;
    TextView textViewLeft;
    TextView textViewRight;
    RangeSeekBar rangeSeekBar;
    
    private String filePath;

    private static final String FILEPATH = "filepath";

    boolean isPlaying = false;

    int duration;
    String filePrefix;
    String[] command;
    File dest;
    String original_path;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trim);
        imageView = findViewById(R.id.pause);
        videoView = findViewById(R.id.videoView);
        textViewLeft = findViewById(R.id.tvLeft);
        textViewRight = findViewById(R.id.tvRight);
        rangeSeekBar = findViewById(R.id.seekbar);

        Intent i = getIntent();
        if (i != null) {
            String imagePath = i.getStringExtra("uri");
            uri = Uri.parse(imagePath);
            isPlaying = true;
            videoView.setVideoURI(uri);
            videoView.start();
        }
        setListeners();
    }

    private void setListeners() {
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPlaying) {
                    imageView.setImageResource(R.drawable.ic_play);
                    videoView.pause();
                    isPlaying = false;
                } else {
                    videoView.start();
                    imageView.setImageResource(R.drawable.ic_pause);
                    isPlaying = true;
                }
            }
        });

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                videoView.start();
                duration = mediaPlayer.getDuration() / 1000;

                textViewLeft.setText("00:00:00");

                textViewRight.setText(getTime(mediaPlayer.getDuration() / 1000));
                mediaPlayer.setLooping(true);
                rangeSeekBar.setRangeValues(0, duration);
                rangeSeekBar.setSelectedMaxValue(duration);
                rangeSeekBar.setSelectedMinValue(0);
                rangeSeekBar.setEnabled(true);


                rangeSeekBar.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener() {
                    @Override
                    public void onRangeSeekBarValuesChanged(RangeSeekBar bar, Object minValue, Object maxValue) {
                        videoView.seekTo((int) minValue * 1000);

                        textViewLeft.setText(getTime((int) bar.getSelectedMinValue()));
                        textViewRight.setText(getTime((int) bar.getSelectedMaxValue()));
                    }
                });
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (videoView.getCurrentPosition() >= rangeSeekBar.getSelectedMaxValue().intValue() * 1000) {
                            videoView.seekTo(rangeSeekBar.getSelectedMinValue().intValue() * 1000);
                        }

                    }
                }, 1000);
            }
        });
    }


    private String getTime(int seconds) {
        int hr = seconds / 3600;
        int rem = seconds % 3600;
        int mn = rem / 60;
        int sec = rem % 60;
        return format("%02d", hr) + ":" + format("%02d", mn) + ":" + format("%02d", sec);

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.trimVideo) {


            final AlertDialog.Builder alert = new AlertDialog.Builder(TrimActivity.this);

            LinearLayout linearLayout = new LinearLayout(TrimActivity.this);
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(50, 0, 50, 100);
            final EditText input = new EditText(TrimActivity.this);
            input.setLayoutParams(lp);
            input.setGravity(Gravity.TOP | Gravity.START);
            input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            linearLayout.addView(input, lp);
            alert.setMessage("set video name?");
            alert.setTitle("video name");
            alert.setView(linearLayout);
            alert.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });

            alert.setPositiveButton("submit", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    filePrefix = input.getText().toString();

                    trimVideo(rangeSeekBar.getSelectedMinValue().intValue() * 1000,
                            rangeSeekBar.getSelectedMaxValue().intValue() * 1000, filePrefix);


                    finish();
                    dialogInterface.dismiss();

                }
            });
            alert.show();
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        return true;

    }

    private void trimVideo(int startMs, int endMs, String fileName) {
        File folder = new File(Environment.getExternalStorageDirectory() + "/VideoEditor");
        if (!folder.exists()) {
            folder.mkdir();
        }
        filePrefix = fileName;

        String fileExt = ".mp4";
        System.out.println("audio"+fileExt);
        dest = new File(folder, filePrefix + fileExt);
        original_path = getRealPathFromUri(getApplicationContext(), uri);

        duration = (endMs - startMs) / 1000;
        filePath = dest.getAbsolutePath();

        command = new String[]{"-ss", "" + startMs / 1000, "-y", "-i", original_path, "-t", "" + (endMs - startMs) / 1000, "-vcodec", "mpeg4", "-b:v", "2097152", "-b:a", "48000", "-ac", "2", "-ar", "22050", filePath};
        execffmpegBinary(command);

    }

    private String getRealPathFromUri(Context context, Uri contentUri) {

        Cursor cursor = null;
        try {

            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

            cursor.moveToFirst();
            return cursor.getString(column_index);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
    private void execffmpegBinary(final String[] command) {
        Config.enableLogCallback(new LogCallback() {
            @Override
            public void apply(LogMessage message) {
                Log.e(Config.TAG, message.getText());
            }
        });

        Config.enableLogCallback(new LogCallback() {
            @Override
            public void apply(LogMessage message) {
                Log.e(Config.TAG, message.getText());
            }
        });

        Config.enableStatisticsCallback(new StatisticsCallback() {
            @Override
            public void apply(Statistics newStatistics) {

            }
        });
        Log.d(TAG, "Started command : ffmpeg " + Arrays.toString(command));


        long executionId = FFmpeg.executeAsync(command, new ExecuteCallback() {
            @Override
            public void apply(long executionId, int returnCode) {
                if (returnCode == RETURN_CODE_SUCCESS) {
                  //  progressDialog.dismiss();
                    Log.d(Config.TAG, "finished command: ffmpeg" + Arrays.toString(command));
                    Intent intent = new Intent(TrimActivity.this, PreviewActivity.class);
                    intent.putExtra(FILEPATH, filePath);
                    startActivity(intent);

                } else if (returnCode == RETURN_CODE_CANCEL) {
                    Log.e(Config.TAG, "Async command execution canceled by user");
                } else {
                    Log.e(Config.TAG, format("Async command execution failed with returncode = %d", returnCode));
                }
            }
        });
    }


    }