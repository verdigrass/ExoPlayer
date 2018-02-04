/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer2.imademo;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;

/**
 * Main Activity for the IMA plugin demo. {@link ExoPlayer} objects are created by
 * {@link PlayerManager}, which this class instantiates.
 */
public final class MainActivity extends Activity {

  private SimpleExoPlayerView playerView;
  private PlayerManager player;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.e("grass", "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "]...");

    setContentView(R.layout.main_activity);


    playerView = findViewById(R.id.player_view);
    player = new PlayerManager(this);
  }



  @Override
  public void onResume() {
    Log.e("grass", "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "]...");
    super.onResume();
    player.init(this, playerView);
  }

  @Override
  public void onPause() {
    Log.e("grass", "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "]...");

    super.onPause();
    player.reset();
  }

  @Override
  public void onDestroy() {
    Log.e("grass", "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "]...");

    player.release();
    super.onDestroy();
  }

}
