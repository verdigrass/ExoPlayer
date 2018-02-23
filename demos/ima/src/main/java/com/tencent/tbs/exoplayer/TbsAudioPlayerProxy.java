package com.tencent.tbs.exoplayer;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;

import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.imademo.R;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;

import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;

//import com.tencent.common.http.MttLocalProxy;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.tencent.smtt.audio.export.IAudioPlayerCallback;
import com.tencent.smtt.audio.export.IMediaPlayer;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Map;

public class TbsAudioPlayerProxy
        implements Player.EventListener, IMediaPlayer
{

    public void onRepeatModeChanged(int repeatMode) {

    }

    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

    }

    public void onTimelineChanged(Timeline paramt, Object paramObject)
    {
    }

    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections)
    {
    }

    private static final String LOGTAG = "TbsAudioPlayerProxy";
    protected static final int PLAYER_STATE_CHANGED = 100;
    private IAudioPlayerCallback mAudioPlayerCallback = null;
    private int mBufferedPercentage = 0;
    Context mContext = null;

    private Handler mHandler = new Handler(Looper.getMainLooper())
    {
        public void handleMessage(Message paramAnonymousMessage)
        {
            boolean bool = true;
            switch (paramAnonymousMessage.what)
            {
                case 100:
                    boolean b = (paramAnonymousMessage.arg1 == 1);

                    int i = paramAnonymousMessage.arg2;
                    TbsAudioPlayerProxy.this.handlePlayerStateChanged(b, i);

                    break;


                default:
                    return;

            }

        }
    };
    boolean mIsLooping = false;
    private MediaPlayer.OnBufferingUpdateListener mOnBufferingUpdateListener = null;
    private MediaPlayer.OnCompletionListener mOnCompletionListener = null;
    private MediaPlayer.OnErrorListener mOnErrorListener = null;
    private MediaPlayer.OnInfoListener mOnInfoListener = null;
    private MediaPlayer.OnPreparedListener mOnPreparedListener = null;
    private MediaPlayer.OnSeekCompleteListener mOnSeekCompleteListener = null;
    SimpleExoPlayer mPlayer = null;


    TrackSelector mTrackSelector = null;
    MediaSource mediaSource = null;
    private boolean notifiedCompletion = false;
    private boolean notifiedPrepared = false;
    private boolean notifiedSeekCompleted;
    SimpleExoPlayerView player_view = null;

    public TbsAudioPlayerProxy(Context paramContext)
    {
        if (paramContext == null)
            throw new RuntimeException("Input context is null!");

        // Create a default track selector.
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);


        this.mContext = paramContext.getApplicationContext();
        this.mTrackSelector = trackSelector;
//        this.mPlayer = new SimpleExoPlayer(mContext, mTrackSelector);


        // Create a player instance.
        this.mPlayer = ExoPlayerFactory.newSimpleInstance(mContext, trackSelector);



        this.mPlayer.addListener(this);
        Log.i("TbsAudioPlayerProxy", "TbsAudioPlayerProxy -- ctx:" + paramContext + "; selector: " + this.mTrackSelector + "; player: " + this.mPlayer + "; this: " + this);
        this.notifiedPrepared = false;
        this.notifiedCompletion = false;
        this.notifiedSeekCompleted = false;
    }

    public int getCurrentPosition()
    {
        int i = -1;
        if (this.mPlayer != null)
            i = (int)this.mPlayer.getCurrentPosition();
        return i;
    }

    public int getDuration()
    {
        int i = -1;
        if (this.mPlayer != null)
            i = (int)this.mPlayer.getDuration();
        return i;
    }

    public Object getTrackInfo()
            throws IllegalStateException
    {
        return null;
    }


    /**
     * The player does not have any media to play.
     */
    int STATE_IDLE = 1;
    /**
     * The player is not able to immediately play from its current position. This state typically
     * occurs when more data needs to be loaded.
     */
    int STATE_BUFFERING = 2;
    /**
     * The player is able to immediately play from its current position. The player will be playing if
     * {@link #getPlayWhenReady()} is true, and paused otherwise.
     */
    int STATE_READY = 3;
    /**
     * The player has finished playing the media.
     */
    int STATE_ENDED = 4;


    public void handlePlayerStateChanged(boolean paramBoolean, int paramInt)
    {
        Log.d("TbsAudioPlayerProxy", "@handlePlayerStateChanged (" + paramBoolean + ", " + paramInt + ");  notifiedPrepared: " + this.notifiedPrepared + ", notifiedSeekCompleted: " + this.notifiedSeekCompleted + ", notifiedCompletion: " + this.notifiedCompletion + ", player: " + this.mPlayer + ", this: " + this + ", thread: " + Thread.currentThread().getId());
        switch (paramInt)
        {
            case 3:

                this.notifiedCompletion = false;
                if ((this.mOnPreparedListener != null) && (!this.notifiedPrepared))
                {
                    this.notifiedPrepared = true;
                    Log.d("TbsAudioPlayerProxy", "mOnPreparedListener.onPrepared --> notifiedPrepared: " + this.notifiedPrepared);
                    this.mOnPreparedListener.onPrepared(null);
                }



                if ((this.mOnSeekCompleteListener != null) && (!this.notifiedSeekCompleted)) {
                    this.notifiedSeekCompleted = true;
                    Log.d("TbsAudioPlayerProxy", "mOnSeekCompleteListener.onSeekComplete...");
                    this.mOnSeekCompleteListener.onSeekComplete(null);
                }

                break;

            case 4:


                if ((this.mOnCompletionListener != null) && (!this.notifiedCompletion))
                {
                    Log.d("TbsAudioPlayerProxy", "mOnCompletionListener.onCompletion...");
                    this.mOnCompletionListener.onCompletion(null);
                    this.notifiedCompletion = true;
                }


                break;


            case 2:



                if (this.mOnBufferingUpdateListener != null)
                {
                    int i = this.mPlayer.getBufferedPercentage();
                    this.mBufferedPercentage = i;
                    Log.d("TbsAudioPlayerProxy", "mOnBufferingUpdateListener.onBufferingUpdate: " + i);
                    this.mOnBufferingUpdateListener.onBufferingUpdate(null, i);
                }


                break;


            default:
                break;

        }

        Log.d("TbsAudioPlayerProxy", "#handlePlayerStateChanged (" + paramBoolean + ", " + paramInt + ");  notifiedPrepared: " + this.notifiedPrepared + ", notifiedSeekCompleted: " + this.notifiedSeekCompleted + ", notifiedCompletion: " + this.notifiedCompletion + ", player: " + this.mPlayer + ", this: " + this + ", thread: " + Thread.currentThread().getId());


    }

    public boolean isLooping()
    {
        return this.mIsLooping;
    }

    public boolean isPlaying()
    {
        boolean bool = false;
        if (this.mPlayer != null)
            bool = this.mPlayer.getPlayWhenReady();
        return bool;
    }

    public void onLoadingChanged(boolean paramBoolean)
    {
        Log.d("TbsAudioPlayerProxy", "onLoadingChanged :: player: " + this.mPlayer + ", isloading: " + paramBoolean);
        if ((this.mBufferedPercentage < 100) && (this.mOnBufferingUpdateListener != null))
        {
            this.mBufferedPercentage = 100;
            Log.d("TbsAudioPlayerProxy", "mOnBufferingUpdateListener.onBufferingUpdate: " + this.mBufferedPercentage);
            this.mOnBufferingUpdateListener.onBufferingUpdate(null, this.mBufferedPercentage);
        }
    }

    public void onPlaybackParametersChanged(PlaybackParameters paramn)
    {
        Log.d("TbsAudioPlayerProxy", "onPlaybackParametersChanged -- params: (" + paramn.speed
                + ", " + paramn.pitch + ");");
    }

    public void onPlayerError(ExoPlaybackException parame)
    {
        String str;
        int i = 0;
        if (parame != null)
        {
            str = parame.getMessage();
            if (str != null)
                i = str.hashCode();
            else
                i = parame.hashCode();
        }

            if ((this.mOnErrorListener != null) && (parame != null))
                this.mOnErrorListener.onError(null, i, 0);
            str = Log.getStackTraceString(parame);
            if ((this.mAudioPlayerCallback != null) && (parame != null))
                this.mAudioPlayerCallback.onPlayerMessage(i, str);
            Log.e("TbsAudioPlayerProxy", "onPlayerError ( player: " + this.mPlayer + ", " + str + " )");

            return;

    }

    public void onPlayerStateChanged(boolean paramBoolean, int paramInt)
    {
        Log.d("TbsAudioPlayerProxy", "onPlayerStateChanged (" + paramBoolean + ", " + paramInt + ");  notifiedPrepared: " + this.notifiedPrepared + ", notifiedSeekCompleted: " + this.notifiedSeekCompleted + ", notifiedCompletion: " + this.notifiedCompletion + ", player: " + this.mPlayer + ", this: " + this + ", thread: " + Thread.currentThread().getId());
        Handler localHandler = this.mHandler;
        if (paramBoolean);
        for (int i = 1; ; i = 0)
        {
            localHandler.obtainMessage(100, i, paramInt).sendToTarget();
            return;
        }
    }

    public void onPositionDiscontinuity()
    {
    }


    public void pause()
            throws IllegalStateException
    {
        Log.d("TbsAudioPlayerProxy", "pause, player: " + this.mPlayer);
        this.mPlayer.setPlayWhenReady(false);
    }

    public void prepare()
            throws IOException, IllegalStateException
    {
        Log.d("TbsAudioPlayerProxy", "prepare with ms: " + this.mediaSource + ", player: " + this.mPlayer);
        this.mPlayer.seekTo(0);

        this.mPlayer.prepare(this.mediaSource);
        this.mPlayer.setPlayWhenReady(false);
        this.notifiedPrepared = false;
        this.notifiedCompletion = false;
        this.notifiedSeekCompleted = false;
    }

    public void prepareAsync()
            throws IllegalStateException
    {
        Log.d("TbsAudioPlayerProxy", "prepareAsync with ms: " + this.mediaSource + ", player: " + this.mPlayer);
        this.mPlayer.prepare(this.mediaSource);


        this.mPlayer.setPlayWhenReady(false);


        this.notifiedPrepared = false;
        this.notifiedCompletion = false;
        this.notifiedSeekCompleted = false;
    }

    public void release()
    {
        Log.d("TbsAudioPlayerProxy", "release , player: " + this.mPlayer);
        if (this.mPlayer != null)
        {
            this.mPlayer.release();
            this.mPlayer = null;
        }
    }

    public void reset()
    {
        Log.d("TbsAudioPlayerProxy", "reset , player: " + this.mPlayer, new Throwable("why?"));
        if (this.mPlayer != null)
        {
            long l = this.mPlayer.getCurrentPosition();
            Log.d("TbsAudioPlayerProxy", "reset player: " + this.mPlayer + ", pos: " + l);
        }
    }

    public void seekTo(int paramInt)
            throws IllegalStateException
    {
        Log.d("TbsAudioPlayerProxy", "seekTo " + paramInt + ", player: " + this.mPlayer);
        this.notifiedSeekCompleted = false;
        this.mPlayer.seekTo(paramInt);
    }

    public void setAudioStreamType(int paramInt)
    {
    }

    public void setCallback(IAudioPlayerCallback paramIAudioPlayerCallback)
    {
        this.mAudioPlayerCallback = paramIAudioPlayerCallback;
    }

    public void setDataSource(Context paramContext, Uri paramUri)
            throws IOException, IllegalArgumentException, SecurityException, IllegalStateException
    {
        Log.d("TbsAudioPlayerProxy", "setDataSource#1 " + paramUri);
        setDataSource(paramContext, paramUri, null);
    }

    public void setDataSource(Context paramContext, Uri paramUri, Map<String, String> paramMap)
            throws IOException, IllegalArgumentException, SecurityException, IllegalStateException
    {
        Log.d("TbsAudioPlayerProxy", "setDataSource#2 " + paramUri + ", this: " + this + ", player: " + this.mPlayer);


        // Produces DataSource instances through which media data is loaded.
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(mContext,
                Util.getUserAgent(mContext, "TbsMedia"));

        // Produces Extractor instances for parsing the content media (i.e. not the ad).
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();

        // This is the MediaSource representing the content media (i.e. not the ad).
//        String contentUrl = mContext.getString(R.string.content_url);
        this.mediaSource = new ExtractorMediaSource(
                paramUri, dataSourceFactory, extractorsFactory,
                null, null);

//
//        this.mediaSource = new b(Uri.parse(paramUri.toString()),
//                new l(this.mContext, com.google.android.exoplayer2.h.s.a(paramContext, "TbsMedia")),
//                new com.google.android.exoplayer2.c.c(), null, null);

        this.notifiedPrepared = false;
        this.notifiedCompletion = false;
        this.notifiedSeekCompleted = false;
    }

    public void setDataSource(FileDescriptor paramFileDescriptor)
            throws IOException, IllegalArgumentException, IllegalStateException
    {
    }

    public void setDataSource(FileDescriptor paramFileDescriptor, long paramLong1, long paramLong2)
            throws IOException, IllegalArgumentException, IllegalStateException
    {
    }

    public void setDataSource(String paramString)
            throws IOException, IllegalArgumentException, SecurityException, IllegalStateException
    {
        Log.d("TbsAudioPlayerProxy", "setDataSource#3: " + paramString + ", player: " + this.mPlayer);
        Uri uri = Uri.parse(paramString);
        setDataSource(this.mContext, uri, null);
    }

    public void setLooping(boolean paramBoolean)
    {
        Log.d("TbsAudioPlayerProxy", "setLooping(" + paramBoolean + "), player: " + this.mPlayer);
        this.mIsLooping = paramBoolean;
    }

    public void setOnBufferingUpdateListener(MediaPlayer.OnBufferingUpdateListener paramOnBufferingUpdateListener)
    {
        this.mOnBufferingUpdateListener = paramOnBufferingUpdateListener;
    }

    public void setOnCompletionListener(MediaPlayer.OnCompletionListener paramOnCompletionListener)
    {
        this.mOnCompletionListener = paramOnCompletionListener;
    }

    public void setOnErrorListener(MediaPlayer.OnErrorListener paramOnErrorListener)
    {
        this.mOnErrorListener = paramOnErrorListener;
    }

    public void setOnInfoListener(MediaPlayer.OnInfoListener paramOnInfoListener)
    {
        this.mOnInfoListener = paramOnInfoListener;
    }

    public void setOnPreparedListener(MediaPlayer.OnPreparedListener paramOnPreparedListener)
    {
        this.mOnPreparedListener = paramOnPreparedListener;
    }

    public void setOnSeekCompleteListener(MediaPlayer.OnSeekCompleteListener paramOnSeekCompleteListener)
    {
        this.mOnSeekCompleteListener = paramOnSeekCompleteListener;
    }

    public void setPlaySpeed(float paramFloat)
    {
        Log.d("TbsAudioPlayerProxy", "setPlaySpeed (" + paramFloat + ")...");
        PlaybackParameters localn = new  PlaybackParameters (paramFloat, 1.0F);
        this.mPlayer.setPlaybackParameters(localn);
    }

    public void setView(SimpleExoPlayerView paramSimpleExoPlayerView)
    {
        this.player_view = paramSimpleExoPlayerView;
        paramSimpleExoPlayerView.setPlayer(this.mPlayer);
    }

    public void setVolume(float paramFloat1, float paramFloat2)
    {
        this.mPlayer.setVolume((paramFloat1 + paramFloat2) / 2.0F);
    }

    public void setWakeMode(Context paramContext, int paramInt)
    {
    }

    public void start()
            throws IllegalStateException
    {
        Log.d("TbsAudioPlayerProxy", "start, player: " + this.mPlayer);
        this.mPlayer.setPlayWhenReady(true);
    }

    public void stop()
            throws IllegalStateException
    {
        Log.d("TbsAudioPlayerProxy", "stop, player: " + this.mPlayer);
        this.mPlayer.release();
    }

    public void onSeekProcessed() {

    }

    public void onPositionDiscontinuity(@Player.DiscontinuityReason int reason) {
        // Do nothing.
    }
}