package com.tencent.smtt.audio.export;


import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.net.Proxy;

import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.PowerManager;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaFormat;


import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Runnable;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;
import java.lang.ref.WeakReference;


/**
 * Created by squirrel on 18-2-22.
 */

public interface IMediaPlayer {



    /**
     * MediaPlayer class can be used to control playback
     * of audio/video files and streams. An example on how to use the methods in
     * this class can be found in {@link android.widget.VideoView}.
     *
     * <p>Topics covered here are:
     * <ol>
     * <li><a href="#StateDiagram">State Diagram</a>
     * <li><a href="#Valid_and_Invalid_States">Valid and Invalid States</a>
     * <li><a href="#Permissions">Permissions</a>
     * <li><a href="#Callbacks">Register informational and error callbacks</a>
     * </ol>
     *
     * <div class="special reference">
     * <h3>Developer Guides</h3>
     * <p>For more information about how to use MediaPlayer, read the
     * <a href="{@docRoot}guide/topics/media/mediaplayer.html">Media Playback</a> developer guide.</p>
     * </div>
     *
     * <a name="StateDiagram"></a>
     * <h3>State Diagram</h3>
     *
     * <p>Playback control of audio/video files and streams is managed as a state
     * machine. The following diagram shows the life cycle and the states of a
     * MediaPlayer object driven by the supported playback control operations.
     * The ovals represent the states a MediaPlayer object may reside
     * in. The arcs represent the playback control operations that drive the object
     * state transition. There are two types of arcs. The arcs with a single arrow
     * head represent synchronous method calls, while those with
     * a double arrow head represent asynchronous method calls.</p>
     *
     * <p><img src="../../../images/mediaplayer_state_diagram.gif"
     *         alt="MediaPlayer State diagram"
     *         border="0" /></p>
     *
     * <p>From this state diagram, one can see that a MediaPlayer object has the
     *    following states:</p>
     * <ul>
     *     <li>When a MediaPlayer object is just created using <code>new</code> or
     *         after {@link #reset()} is called, it is in the <em>Idle</em> state; and after
     *         {@link #release()} is called, it is in the <em>End</em> state. Between these
     *         two states is the life cycle of the MediaPlayer object.
     *         <ul>
     *         <li>There is a subtle but important difference between a newly constructed
     *         MediaPlayer object and the MediaPlayer object after {@link #reset()}
     *         is called. It is a programming error to invoke methods such
     *         as {@link #getCurrentPosition()},
     *         {@link #getDuration()}, {@link #getVideoHeight()},
     *         {@link #getVideoWidth()}, {@link #setAudioStreamType(int)},
     *         {@link #setLooping(boolean)},
     *         {@link #setVolume(float, float)}, {@link #pause()}, {@link #start()},
     *         {@link #stop()}, {@link #seekTo(int)}, {@link #prepare()} or
     *         {@link #prepareAsync()} in the <em>Idle</em> state for both cases. If any of these
     *         methods is called right after a MediaPlayer object is constructed,
     *         the user supplied callback method OnErrorListener.onError() won't be
     *         called by the internal player engine and the object state remains
     *         unchanged; but if these methods are called right after {@link #reset()},
     *         the user supplied callback method OnErrorListener.onError() will be
     *         invoked by the internal player engine and the object will be
     *         transfered to the <em>Error</em> state. </li>
     *         <li>It is also recommended that once
     *         a MediaPlayer object is no longer being used, call {@link #release()} immediately
     *         so that resources used by the internal player engine associated with the
     *         MediaPlayer object can be released immediately. Resource may include
     *         singleton resources such as hardware acceleration components and
     *         failure to call {@link #release()} may cause subsequent instances of
     *         MediaPlayer objects to fallback to software implementations or fail
     *         altogether. Once the MediaPlayer
     *         object is in the <em>End</em> state, it can no longer be used and
     *         there is no way to bring it back to any other state. </li>
     *         <li>Furthermore,
     *         the MediaPlayer objects created using <code>new</code> is in the
     *         <em>Idle</em> state, while those created with one
     *         of the overloaded convenient <code>create</code> methods are <em>NOT</em>
     *         in the <em>Idle</em> state. In fact, the objects are in the <em>Prepared</em>
     *         state if the creation using <code>create</code> method is successful.
     *         </li>
     *         </ul>
     *         </li>
     *     <li>In general, some playback control operation may fail due to various
     *         reasons, such as unsupported audio/video format, poorly interleaved
     *         audio/video, resolution too high, streaming timeout, and the like.
     *         Thus, error reporting and recovery is an important concern under
     *         these circumstances. Sometimes, due to programming errors, invoking a playback
     *         control operation in an invalid state may also occur. Under all these
     *         error conditions, the internal player engine invokes a user supplied
     *         OnErrorListener.onError() method if an OnErrorListener has been
     *         registered beforehand via
     *         {@link #setOnErrorListener(android.media.MediaPlayer.OnErrorListener)}.
     *         <ul>
     *         <li>It is important to note that once an error occurs, the
     *         MediaPlayer object enters the <em>Error</em> state (except as noted
     *         above), even if an error listener has not been registered by the application.</li>
     *         <li>In order to reuse a MediaPlayer object that is in the <em>
     *         Error</em> state and recover from the error,
     *         {@link #reset()} can be called to restore the object to its <em>Idle</em>
     *         state.</li>
     *         <li>It is good programming practice to have your application
     *         register a OnErrorListener to look out for error notifications from
     *         the internal player engine.</li>
     *         <li>IllegalStateException is
     *         thrown to prevent programming errors such as calling {@link #prepare()},
     *         {@link #prepareAsync()}, or one of the overloaded <code>setDataSource
     *         </code> methods in an invalid state. </li>
     *         </ul>
     *         </li>
     *     <li>Calling
     *         {@link #setDataSource(FileDescriptor)}, or
     *         {@link #setDataSource(String)}, or
     *         {@link #setDataSource(Context, Uri)}, or
     *         {@link #setDataSource(FileDescriptor, long, long)} transfers a
     *         MediaPlayer object in the <em>Idle</em> state to the
     *         <em>Initialized</em> state.
     *         <ul>
     *         <li>An IllegalStateException is thrown if
     *         setDataSource() is called in any other state.</li>
     *         <li>It is good programming
     *         practice to always look out for <code>IllegalArgumentException</code>
     *         and <code>IOException</code> that may be thrown from the overloaded
     *         <code>setDataSource</code> methods.</li>
     *         </ul>
     *         </li>
     *     <li>A MediaPlayer object must first enter the <em>Prepared</em> state
     *         before playback can be started.
     *         <ul>
     *         <li>There are two ways (synchronous vs.
     *         asynchronous) that the <em>Prepared</em> state can be reached:
     *         either a call to {@link #prepare()} (synchronous) which
     *         transfers the object to the <em>Prepared</em> state once the method call
     *         returns, or a call to {@link #prepareAsync()} (asynchronous) which
     *         first transfers the object to the <em>Preparing</em> state after the
     *         call returns (which occurs almost right way) while the internal
     *         player engine continues working on the rest of preparation work
     *         until the preparation work completes. When the preparation completes or when {@link #prepare()} call returns,
     *         the internal player engine then calls a user supplied callback method,
     *         onPrepared() of the OnPreparedListener interface, if an
     *         OnPreparedListener is registered beforehand via {@link
     *         #setOnPreparedListener(android.media.MediaPlayer.OnPreparedListener)}.</li>
     *         <li>It is important to note that
     *         the <em>Preparing</em> state is a transient state, and the behavior
     *         of calling any method with side effect while a MediaPlayer object is
     *         in the <em>Preparing</em> state is undefined.</li>
     *         <li>An IllegalStateException is
     *         thrown if {@link #prepare()} or {@link #prepareAsync()} is called in
     *         any other state.</li>
     *         <li>While in the <em>Prepared</em> state, properties
     *         such as audio/sound volume, screenOnWhilePlaying, looping can be
     *         adjusted by invoking the corresponding set methods.</li>
     *         </ul>
     *         </li>
     *     <li>To start the playback, {@link #start()} must be called. After
     *         {@link #start()} returns successfully, the MediaPlayer object is in the
     *         <em>Started</em> state. {@link #isPlaying()} can be called to test
     *         whether the MediaPlayer object is in the <em>Started</em> state.
     *         <ul>
     *         <li>While in the <em>Started</em> state, the internal player engine calls
     *         a user supplied OnBufferingUpdateListener.onBufferingUpdate() callback
     *         method if a OnBufferingUpdateListener has been registered beforehand
     *         via {@link #setOnBufferingUpdateListener(OnBufferingUpdateListener)}.
     *         This callback allows applications to keep track of the buffering status
     *         while streaming audio/video.</li>
     *         <li>Calling {@link #start()} has not effect
     *         on a MediaPlayer object that is already in the <em>Started</em> state.</li>
     *         </ul>
     *         </li>
     *     <li>Playback can be paused and stopped, and the current playback position
     *         can be adjusted. Playback can be paused via {@link #pause()}. When the call to
     *         {@link #pause()} returns, the MediaPlayer object enters the
     *         <em>Paused</em> state. Note that the transition from the <em>Started</em>
     *         state to the <em>Paused</em> state and vice versa happens
     *         asynchronously in the player engine. It may take some time before
     *         the state is updated in calls to {@link #isPlaying()}, and it can be
     *         a number of seconds in the case of streamed content.
     *         <ul>
     *         <li>Calling {@link #start()} to resume playback for a paused
     *         MediaPlayer object, and the resumed playback
     *         position is the same as where it was paused. When the call to
     *         {@link #start()} returns, the paused MediaPlayer object goes back to
     *         the <em>Started</em> state.</li>
     *         <li>Calling {@link #pause()} has no effect on
     *         a MediaPlayer object that is already in the <em>Paused</em> state.</li>
     *         </ul>
     *         </li>
     *     <li>Calling  {@link #stop()} stops playback and causes a
     *         MediaPlayer in the <em>Started</em>, <em>Paused</em>, <em>Prepared
     *         </em> or <em>PlaybackCompleted</em> state to enter the
     *         <em>Stopped</em> state.
     *         <ul>
     *         <li>Once in the <em>Stopped</em> state, playback cannot be started
     *         until {@link #prepare()} or {@link #prepareAsync()} are called to set
     *         the MediaPlayer object to the <em>Prepared</em> state again.</li>
     *         <li>Calling {@link #stop()} has no effect on a MediaPlayer
     *         object that is already in the <em>Stopped</em> state.</li>
     *         </ul>
     *         </li>
     *     <li>The playback position can be adjusted with a call to
     *         {@link #seekTo(int)}.
     *         <ul>
     *         <li>Although the asynchronuous {@link #seekTo(int)}
     *         call returns right way, the actual seek operation may take a while to
     *         finish, especially for audio/video being streamed. When the actual
     *         seek operation completes, the internal player engine calls a user
     *         supplied OnSeekComplete.onSeekComplete() if an OnSeekCompleteListener
     *         has been registered beforehand via
     *         {@link #setOnSeekCompleteListener(OnSeekCompleteListener)}.</li>
     *         <li>Please
     *         note that {@link #seekTo(int)} can also be called in the other states,
     *         such as <em>Prepared</em>, <em>Paused</em> and <em>PlaybackCompleted
     *         </em> state.</li>
     *         <li>Furthermore, the actual current playback position
     *         can be retrieved with a call to {@link #getCurrentPosition()}, which
     *         is helpful for applications such as a Music player that need to keep
     *         track of the playback progress.</li>
     *         </ul>
     *         </li>
     *     <li>When the playback reaches the end of stream, the playback completes.
     *         <ul>
     *         <li>If the looping mode was being set to <var>true</var>with
     *         {@link #setLooping(boolean)}, the MediaPlayer object shall remain in
     *         the <em>Started</em> state.</li>
     *         <li>If the looping mode was set to <var>false
     *         </var>, the player engine calls a user supplied callback method,
     *         OnCompletion.onCompletion(), if a OnCompletionListener is registered
     *         beforehand via {@link #setOnCompletionListener(OnCompletionListener)}.
     *         The invoke of the callback signals that the object is now in the <em>
     *         PlaybackCompleted</em> state.</li>
     *         <li>While in the <em>PlaybackCompleted</em>
     *         state, calling {@link #start()} can restart the playback from the
     *         beginning of the audio/video source.</li>
     * </ul>
     *
     *
     * <a name="Valid_and_Invalid_States"></a>
     * <h3>Valid and invalid states</h3>
     *
     * <table border="0" cellspacing="0" cellpadding="0">
     * <tr><td>Method Name </p></td>
     *     <td>Valid Sates </p></td>
     *     <td>Invalid States </p></td>
     *     <td>Comments </p></td></tr>
     * <tr><td>attachAuxEffect </p></td>
     *     <td>{Initialized, Prepared, Started, Paused, Stopped, PlaybackCompleted} </p></td>
     *     <td>{Idle, Error} </p></td>
     *     <td>This method must be called after setDataSource.
     *     Calling it does not change the object state. </p></td></tr>
     * <tr><td>getAudioSessionId </p></td>
     *     <td>any </p></td>
     *     <td>{} </p></td>
     *     <td>This method can be called in any state and calling it does not change
     *         the object state. </p></td></tr>
     * <tr><td>getCurrentPosition </p></td>
     *     <td>{Idle, Initialized, Prepared, Started, Paused, Stopped,
     *         PlaybackCompleted} </p></td>
     *     <td>{Error}</p></td>
     *     <td>Successful invoke of this method in a valid state does not change the
     *         state. Calling this method in an invalid state transfers the object
     *         to the <em>Error</em> state. </p></td></tr>
     * <tr><td>getDuration </p></td>
     *     <td>{Prepared, Started, Paused, Stopped, PlaybackCompleted} </p></td>
     *     <td>{Idle, Initialized, Error} </p></td>
     *     <td>Successful invoke of this method in a valid state does not change the
     *         state. Calling this method in an invalid state transfers the object
     *         to the <em>Error</em> state. </p></td></tr>
     * <tr><td>getVideoHeight </p></td>
     *     <td>{Idle, Initialized, Prepared, Started, Paused, Stopped,
     *         PlaybackCompleted}</p></td>
     *     <td>{Error}</p></td>
     *     <td>Successful invoke of this method in a valid state does not change the
     *         state. Calling this method in an invalid state transfers the object
     *         to the <em>Error</em> state.  </p></td></tr>
     * <tr><td>getVideoWidth </p></td>
     *     <td>{Idle, Initialized, Prepared, Started, Paused, Stopped,
     *         PlaybackCompleted}</p></td>
     *     <td>{Error}</p></td>
     *     <td>Successful invoke of this method in a valid state does not change
     *         the state. Calling this method in an invalid state transfers the
     *         object to the <em>Error</em> state. </p></td></tr>
     * <tr><td>isPlaying </p></td>
     *     <td>{Idle, Initialized, Prepared, Started, Paused, Stopped,
     *          PlaybackCompleted}</p></td>
     *     <td>{Error}</p></td>
     *     <td>Successful invoke of this method in a valid state does not change
     *         the state. Calling this method in an invalid state transfers the
     *         object to the <em>Error</em> state. </p></td></tr>
     * <tr><td>pause </p></td>
     *     <td>{Started, Paused, PlaybackCompleted}</p></td>
     *     <td>{Idle, Initialized, Prepared, Stopped, Error}</p></td>
     *     <td>Successful invoke of this method in a valid state transfers the
     *         object to the <em>Paused</em> state. Calling this method in an
     *         invalid state transfers the object to the <em>Error</em> state.</p></td></tr>
     * <tr><td>prepare </p></td>
     *     <td>{Initialized, Stopped} </p></td>
     *     <td>{Idle, Prepared, Started, Paused, PlaybackCompleted, Error} </p></td>
     *     <td>Successful invoke of this method in a valid state transfers the
     *         object to the <em>Prepared</em> state. Calling this method in an
     *         invalid state throws an IllegalStateException.</p></td></tr>
     * <tr><td>prepareAsync </p></td>
     *     <td>{Initialized, Stopped} </p></td>
     *     <td>{Idle, Prepared, Started, Paused, PlaybackCompleted, Error} </p></td>
     *     <td>Successful invoke of this method in a valid state transfers the
     *         object to the <em>Preparing</em> state. Calling this method in an
     *         invalid state throws an IllegalStateException.</p></td></tr>
     * <tr><td>release </p></td>
     *     <td>any </p></td>
     *     <td>{} </p></td>
     *     <td>After {@link #release()}, the object is no longer available. </p></td></tr>
     * <tr><td>reset </p></td>
     *     <td>{Idle, Initialized, Prepared, Started, Paused, Stopped,
     *         PlaybackCompleted, Error}</p></td>
     *     <td>{}</p></td>
     *     <td>After {@link #reset()}, the object is like being just created.</p></td></tr>
     * <tr><td>seekTo </p></td>
     *     <td>{Prepared, Started, Paused, PlaybackCompleted} </p></td>
     *     <td>{Idle, Initialized, Stopped, Error}</p></td>
     *     <td>Successful invoke of this method in a valid state does not change
     *         the state. Calling this method in an invalid state transfers the
     *         object to the <em>Error</em> state. </p></td></tr>
     * <tr><td>setAudioSessionId </p></td>
     *     <td>{Idle} </p></td>
     *     <td>{Initialized, Prepared, Started, Paused, Stopped, PlaybackCompleted,
     *          Error} </p></td>
     *     <td>This method must be called in idle state as the audio session ID must be known before
     *         calling setDataSource. Calling it does not change the object state. </p></td></tr>
     * <tr><td>setAudioStreamType </p></td>
     *     <td>{Idle, Initialized, Stopped, Prepared, Started, Paused,
     *          PlaybackCompleted}</p></td>
     *     <td>{Error}</p></td>
     *     <td>Successful invoke of this method does not change the state. In order for the
     *         target audio stream type to become effective, this method must be called before
     *         prepare() or prepareAsync().</p></td></tr>
     * <tr><td>setAuxEffectSendLevel </p></td>
     *     <td>any</p></td>
     *     <td>{} </p></td>
     *     <td>Calling this method does not change the object state. </p></td></tr>
     * <tr><td>setDataSource </p></td>
     *     <td>{Idle} </p></td>
     *     <td>{Initialized, Prepared, Started, Paused, Stopped, PlaybackCompleted,
     *          Error} </p></td>
     *     <td>Successful invoke of this method in a valid state transfers the
     *         object to the <em>Initialized</em> state. Calling this method in an
     *         invalid state throws an IllegalStateException.</p></td></tr>
     * <tr><td>setDisplay </p></td>
     *     <td>any </p></td>
     *     <td>{} </p></td>
     *     <td>This method can be called in any state and calling it does not change
     *         the object state. </p></td></tr>
     * <tr><td>setSurface </p></td>
     *     <td>any </p></td>
     *     <td>{} </p></td>
     *     <td>This method can be called in any state and calling it does not change
     *         the object state. </p></td></tr>
     * <tr><td>setVideoScalingMode </p></td>
     *     <td>{Initialized, Prepared, Started, Paused, Stopped, PlaybackCompleted} </p></td>
     *     <td>{Idle, Error}</p></td>
     *     <td>Successful invoke of this method does not change the state.</p></td></tr>
     * <tr><td>setLooping </p></td>
     *     <td>{Idle, Initialized, Stopped, Prepared, Started, Paused,
     *         PlaybackCompleted}</p></td>
     *     <td>{Error}</p></td>
     *     <td>Successful invoke of this method in a valid state does not change
     *         the state. Calling this method in an
     *         invalid state transfers the object to the <em>Error</em> state.</p></td></tr>
     * <tr><td>isLooping </p></td>
     *     <td>any </p></td>
     *     <td>{} </p></td>
     *     <td>This method can be called in any state and calling it does not change
     *         the object state. </p></td></tr>
     * <tr><td>setOnBufferingUpdateListener </p></td>
     *     <td>any </p></td>
     *     <td>{} </p></td>
     *     <td>This method can be called in any state and calling it does not change
     *         the object state. </p></td></tr>
     * <tr><td>setOnCompletionListener </p></td>
     *     <td>any </p></td>
     *     <td>{} </p></td>
     *     <td>This method can be called in any state and calling it does not change
     *         the object state. </p></td></tr>
     * <tr><td>setOnErrorListener </p></td>
     *     <td>any </p></td>
     *     <td>{} </p></td>
     *     <td>This method can be called in any state and calling it does not change
     *         the object state. </p></td></tr>
     * <tr><td>setOnPreparedListener </p></td>
     *     <td>any </p></td>
     *     <td>{} </p></td>
     *     <td>This method can be called in any state and calling it does not change
     *         the object state. </p></td></tr>
     * <tr><td>setOnSeekCompleteListener </p></td>
     *     <td>any </p></td>
     *     <td>{} </p></td>
     *     <td>This method can be called in any state and calling it does not change
     *         the object state. </p></td></tr>
     * <tr><td>setScreenOnWhilePlaying</></td>
     *     <td>any </p></td>
     *     <td>{} </p></td>
     *     <td>This method can be called in any state and calling it does not change
     *         the object state.  </p></td></tr>
     * <tr><td>setVolume </p></td>
     *     <td>{Idle, Initialized, Stopped, Prepared, Started, Paused,
     *          PlaybackCompleted}</p></td>
     *     <td>{Error}</p></td>
     *     <td>Successful invoke of this method does not change the state.
     * <tr><td>setWakeMode </p></td>
     *     <td>any </p></td>
     *     <td>{} </p></td>
     *     <td>This method can be called in any state and calling it does not change
     *         the object state.</p></td></tr>
     * <tr><td>start </p></td>
     *     <td>{Prepared, Started, Paused, PlaybackCompleted}</p></td>
     *     <td>{Idle, Initialized, Stopped, Error}</p></td>
     *     <td>Successful invoke of this method in a valid state transfers the
     *         object to the <em>Started</em> state. Calling this method in an
     *         invalid state transfers the object to the <em>Error</em> state.</p></td></tr>
     * <tr><td>stop </p></td>
     *     <td>{Prepared, Started, Stopped, Paused, PlaybackCompleted}</p></td>
     *     <td>{Idle, Initialized, Error}</p></td>
     *     <td>Successful invoke of this method in a valid state transfers the
     *         object to the <em>Stopped</em> state. Calling this method in an
     *         invalid state transfers the object to the <em>Error</em> state.</p></td></tr>
     * <tr><td>getTrackInfo </p></td>
     *     <td>{Prepared, Started, Stopped, Paused, PlaybackCompleted}</p></td>
     *     <td>{Idle, Initialized, Error}</p></td>
     *     <td>Successful invoke of this method does not change the state.</p></td></tr>
     * <tr><td>addTimedTextSource </p></td>
     *     <td>{Prepared, Started, Stopped, Paused, PlaybackCompleted}</p></td>
     *     <td>{Idle, Initialized, Error}</p></td>
     *     <td>Successful invoke of this method does not change the state.</p></td></tr>
     * <tr><td>selectTrack </p></td>
     *     <td>{Prepared, Started, Stopped, Paused, PlaybackCompleted}</p></td>
     *     <td>{Idle, Initialized, Error}</p></td>
     *     <td>Successful invoke of this method does not change the state.</p></td></tr>
     * <tr><td>deselectTrack </p></td>
     *     <td>{Prepared, Started, Stopped, Paused, PlaybackCompleted}</p></td>
     *     <td>{Idle, Initialized, Error}</p></td>
     *     <td>Successful invoke of this method does not change the state.</p></td></tr>
     *
     * </table>
     *
     * <a name="Permissions"></a>
     * <h3>Permissions</h3>
     * <p>One may need to declare a corresponding WAKE_LOCK permission {@link
     * android.R.styleable#AndroidManifestUsesPermission &lt;uses-permission&gt;}
     * element.
     *
     * <p>This class requires the {@link android.Manifest.permission#INTERNET} permission
     * when used with network-based content.
     *
     * <a name="Callbacks"></a>
     * <h3>Callbacks</h3>
     * <p>Applications may want to register for informational and error
     * events in order to be informed of some internal state update and
     * possible runtime errors during playback or streaming. Registration for
     * these events is done by properly setting the appropriate listeners (via calls
     * to
     * {@link #setOnPreparedListener(OnPreparedListener)}setOnPreparedListener,
     * {@link #setOnVideoSizeChangedListener(OnVideoSizeChangedListener)}setOnVideoSizeChangedListener,
     * {@link #setOnSeekCompleteListener(OnSeekCompleteListener)}setOnSeekCompleteListener,
     * {@link #setOnCompletionListener(OnCompletionListener)}setOnCompletionListener,
     * {@link #setOnBufferingUpdateListener(OnBufferingUpdateListener)}setOnBufferingUpdateListener,
     * {@link #setOnInfoListener(OnInfoListener)}setOnInfoListener,
     * {@link #setOnErrorListener(OnErrorListener)}setOnErrorListener, etc).
     * In order to receive the respective callback
     * associated with these listeners, applications are required to create
     * MediaPlayer objects on a thread with its own Looper running (main UI
     * thread by default has a Looper running).
     *
     */
        /**
         Constant to retrieve only the new metadata since the last
         call.
         // FIXME: unhide.
         // FIXME: add link to getMetadata(boolean, boolean)
         {@hide}
         */
        public static final boolean METADATA_UPDATE_ONLY = true;

        /**
         Constant to retrieve all the metadata.
         // FIXME: unhide.
         // FIXME: add link to getMetadata(boolean, boolean)
         {@hide}
         */
        public static final boolean METADATA_ALL = false;

        /**
         Constant to enable the metadata filter during retrieval.
         // FIXME: unhide.
         // FIXME: add link to getMetadata(boolean, boolean)
         {@hide}
         */
        public static final boolean APPLY_METADATA_FILTER = true;

        /**
         Constant to disable the metadata filter during retrieval.
         // FIXME: unhide.
         // FIXME: add link to getMetadata(boolean, boolean)
         {@hide}
         */
        public static final boolean BYPASS_METADATA_FILTER = false;


        /**
         * Interface definition for a callback to be invoked when the media
         * source is ready for playback.
         */
        public interface OnPreparedListener
        {
            /**
             * Called when the media file is ready for playback.
             *
             * @param mp the MediaPlayer that is ready for playback
             */
            void onPrepared(MediaPlayer mp);
        }


        /**
         * Interface definition for a callback to be invoked when playback of
         * a media source has completed.
         */
        public interface OnCompletionListener
        {
            /**
             * Called when the end of a media source is reached during playback.
             *
             * @param mp the MediaPlayer that reached the end of the file
             */
            void onCompletion(MediaPlayer mp);
        }


        /**
         * Interface definition of a callback to be invoked indicating buffering
         * status of a media resource being streamed over the network.
         */
        public interface OnBufferingUpdateListener
        {
            /**
             * Called to update status in buffering a media stream received through
             * progressive HTTP download. The received buffering percentage
             * indicates how much of the content has been buffered or played.
             * For example a buffering update of 80 percent when half the content
             * has already been played indicates that the next 30 percent of the
             * content to play has been buffered.
             *
             * @param mp      the MediaPlayer the update pertains to
             * @param percent the percentage (0-100) of the content
             *                that has been buffered or played thus far
             */
            void onBufferingUpdate(MediaPlayer mp, int percent);
        }

        /**
         * Register a callback to be invoked when the status of a network
         * stream's buffer has changed.
         *
         * @param listener the callback that will be run.
         */


        /**
         * Interface definition of a callback to be invoked indicating
         * the completion of a seek operation.
         */
        public interface OnSeekCompleteListener
        {
            /**
             * Called to indicate the completion of a seek operation.
             *
             * @param mp the MediaPlayer that issued the seek operation
             */
            public void onSeekComplete(MediaPlayer mp);
        }

        /**
         * Register a callback to be invoked when a seek operation has been
         * completed.
         *
         * @param listener the callback that will be run
         */

        /**
         * Interface definition of a callback to be invoked when the
         * video size is first known or updated
         */
        public interface OnVideoSizeChangedListener
        {
            /**
             * Called to indicate the video size
             *
             * The video size (width and height) could be 0 if there was no video,
             * no display surface was set, or the value was not determined yet.
             *
             * @param mp        the MediaPlayer associated with this callback
             * @param width     the width of the video
             * @param height    the height of the video
             */
            public void onVideoSizeChanged(MediaPlayer mp, int width, int height);
        }

        /**
         * Register a callback to be invoked when the video size is
         * known or updated.
         *
         * @param listener the callback that will be run
         */



        /**
         * Register a callback to be invoked when a timed text is available
         * for display.
         *
         * @param listener the callback that will be run
         */


    /* Do not change these values without updating their counterparts
     * in include/media/mediaplayer.h!
     */
        /** Unspecified media player error.
         * @see android.media.MediaPlayer.OnErrorListener
         */
        public static final int MEDIA_ERROR_UNKNOWN = 1;

        /** Media server died. In this case, the application must release the
         * MediaPlayer object and instantiate a new one.
         * @see android.media.MediaPlayer.OnErrorListener
         */
        public static final int MEDIA_ERROR_SERVER_DIED = 100;

        /** The video is streamed and its container is not valid for progressive
         * playback i.e the video's index (e.g moov atom) is not at the start of the
         * file.
         * @see android.media.MediaPlayer.OnErrorListener
         */
        public static final int MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK = 200;

        /** File or network related operation errors. */
        public static final int MEDIA_ERROR_IO = -1004;
        /** Bitstream is not conforming to the related coding standard or file spec. */
        public static final int MEDIA_ERROR_MALFORMED = -1007;
        /** Bitstream is conforming to the related coding standard or file spec, but
         * the media framework does not support the feature. */
        public static final int MEDIA_ERROR_UNSUPPORTED = -1010;
        /** Some operation takes too long to complete, usually more than 3-5 seconds. */
        public static final int MEDIA_ERROR_TIMED_OUT = -110;

        /**
         * Interface definition of a callback to be invoked when there
         * has been an error during an asynchronous operation (other errors
         * will throw exceptions at method call time).
         */
        public interface OnErrorListener
        {
            /**
             * Called to indicate an error.
             *
             * @param mp      the MediaPlayer the error pertains to
             * @param what    the type of error that has occurred:
             * <ul>
             * <li>{@link #MEDIA_ERROR_UNKNOWN}
             * <li>{@link #MEDIA_ERROR_SERVER_DIED}
             * </ul>
             * @param extra an extra code, specific to the error. Typically
             * implementation dependent.
             * <ul>
             * <li>{@link #MEDIA_ERROR_IO}
             * <li>{@link #MEDIA_ERROR_MALFORMED}
             * <li>{@link #MEDIA_ERROR_UNSUPPORTED}
             * <li>{@link #MEDIA_ERROR_TIMED_OUT}
             * </ul>
             * @return True if the method handled the error, false if it didn't.
             * Returning false, or not having an OnErrorListener at all, will
             * cause the OnCompletionListener to be called.
             */
            boolean onError(MediaPlayer mp, int what, int extra);
        }




    /* Do not change these values without updating their counterparts
     * in include/media/mediaplayer.h!
     */
        /** Unspecified media player info.
         * @see android.media.MediaPlayer.OnInfoListener
         */
        public static final int MEDIA_INFO_UNKNOWN = 1;

        /** The player was started because it was used as the next player for another
         * player, which just completed playback.
         * @see android.media.MediaPlayer.OnInfoListener
         * @hide
         */
        public static final int MEDIA_INFO_STARTED_AS_NEXT = 2;

        /** The player just pushed the very first video frame for rendering.
         * @see android.media.MediaPlayer.OnInfoListener
         */
        public static final int MEDIA_INFO_VIDEO_RENDERING_START = 3;

        /** The video is too complex for the decoder: it can't decode frames fast
         *  enough. Possibly only the audio plays fine at this stage.
         * @see android.media.MediaPlayer.OnInfoListener
         */
        public static final int MEDIA_INFO_VIDEO_TRACK_LAGGING = 700;

        /** MediaPlayer is temporarily pausing playback internally in order to
         * buffer more data.
         * @see android.media.MediaPlayer.OnInfoListener
         */
        public static final int MEDIA_INFO_BUFFERING_START = 701;

        /** MediaPlayer is resuming playback after filling buffers.
         * @see android.media.MediaPlayer.OnInfoListener
         */
        public static final int MEDIA_INFO_BUFFERING_END = 702;

        /** Bad interleaving means that a media has been improperly interleaved or
         * not interleaved at all, e.g has all the video samples first then all the
         * audio ones. Video is playing but a lot of disk seeks may be happening.
         * @see android.media.MediaPlayer.OnInfoListener
         */
        public static final int MEDIA_INFO_BAD_INTERLEAVING = 800;

        /** The media cannot be seeked (e.g live stream)
         * @see android.media.MediaPlayer.OnInfoListener
         */
        public static final int MEDIA_INFO_NOT_SEEKABLE = 801;

        /** A new set of metadata is available.
         * @see android.media.MediaPlayer.OnInfoListener
         */
        public static final int MEDIA_INFO_METADATA_UPDATE = 802;

        /** A new set of external-only metadata is available.  Used by
         *  JAVA framework to avoid triggering track scanning.
         * @hide
         */
        public static final int MEDIA_INFO_EXTERNAL_METADATA_UPDATE = 803;

        /** Failed to handle timed text track properly.
         * @see android.media.MediaPlayer.OnInfoListener
         *
         * {@hide}
         */
        public static final int MEDIA_INFO_TIMED_TEXT_ERROR = 900;

        /** Subtitle track was not supported by the media framework.
         * @see android.media.MediaPlayer.OnInfoListener
         */
        public static final int MEDIA_INFO_UNSUPPORTED_SUBTITLE = 901;

        /** Reading the subtitle track takes too long.
         * @see android.media.MediaPlayer.OnInfoListener
         */
        public static final int MEDIA_INFO_SUBTITLE_TIMED_OUT = 902;

        /**
         * Interface definition of a callback to be invoked to communicate some
         * info and/or warning about the media or its playback.
         */
        public interface OnInfoListener
        {
            /**
             * Called to indicate an info or a warning.
             *
             * @param mp      the MediaPlayer the info pertains to.
             * @param what    the type of info or warning.
             * <ul>
             * <li>{@link #MEDIA_INFO_UNKNOWN}
             * <li>{@link #MEDIA_INFO_VIDEO_TRACK_LAGGING}
             * <li>{@link #MEDIA_INFO_VIDEO_RENDERING_START}
             * <li>{@link #MEDIA_INFO_BUFFERING_START}
             * <li>{@link #MEDIA_INFO_BUFFERING_END}
             * <li>{@link #MEDIA_INFO_BAD_INTERLEAVING}
             * <li>{@link #MEDIA_INFO_NOT_SEEKABLE}
             * <li>{@link #MEDIA_INFO_METADATA_UPDATE}
             * <li>{@link #MEDIA_INFO_UNSUPPORTED_SUBTITLE}
             * <li>{@link #MEDIA_INFO_SUBTITLE_TIMED_OUT}
             * </ul>
             * @param extra an extra code, specific to the info. Typically
             * implementation dependent.
             * @return True if the method handled the info, false if it didn't.
             * Returning false, or not having an OnErrorListener at all, will
             * cause the info to be discarded.
             */
            boolean onInfo(MediaPlayer mp, int what, int extra);
        }



}
