// This string is autogenerated by ChangeAppSettings.sh, do not change spaces amount
package org.scummvm.sdl;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.content.res.Configuration;


public class MainActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// fullscreen mode
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				   WindowManager.LayoutParams.FLAG_FULLSCREEN); 
		if(Globals.InhibitSuspend)
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
					WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		System.out.println("libSDL: Creating startup screen");
		_layout = new LinearLayout(this);
		_layout.setOrientation(LinearLayout.VERTICAL);
		_layout.setLayoutParams(new LinearLayout.LayoutParams( ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
		_layout2 = new LinearLayout(this);
		_layout2.setLayoutParams(new LinearLayout.LayoutParams( ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		_btn = new Button(this);
		_btn.setLayoutParams(new ViewGroup.LayoutParams( ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		_btn.setText(getResources().getString(R.string.device_change_cfg));
		class onClickListener implements View.OnClickListener
		{
				public MainActivity p;
				onClickListener( MainActivity _p ) { p = _p; }
				public void onClick(View v)
				{
					System.out.println("libSDL: User clicked change phone config button");
					Settings.showConfig(p);
				}
		};
		_btn.setOnClickListener(new onClickListener(this));

		_layout2.addView(_btn);

		_layout.addView(_layout2);
		
		ImageView img = new ImageView(this);

		img.setScaleType(ImageView.ScaleType.FIT_CENTER /* FIT_XY */ );
		img.setImageResource(R.drawable.publisherlogo);
		img.setLayoutParams(new ViewGroup.LayoutParams( ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
		_layout.addView(img);
		
		setContentView(_layout);

		if(mAudioThread == null) // Starting from background (should not happen)
		{
				System.out.println("libSDL: Loading libraries");
			mLoadLibraryStub = new LoadLibrary();
			mAudioThread = new AudioThread(this);
			System.out.println("libSDL: Loading settings");
			Settings.Load(this);
		}

		if( !Settings.settingsChanged )
		{
			System.out.println("libSDL: 3-second timeout in startup screen");
			class Callback implements Runnable
			{
				MainActivity p;
				Callback( MainActivity _p ) { p = _p; }
				public void run()
				{
					try {
						Thread.sleep(3000);
					} catch( InterruptedException e ) {};
					if( Settings.settingsChanged )
						return;
					System.out.println("libSDL: Timeout reached in startup screen, process with downloader");
					p.startDownloader();
				}
			};
			Thread changeConfigAlertThread = null;
			changeConfigAlertThread = new Thread(new Callback(this));
			changeConfigAlertThread.start();
		}
	}

	public void startDownloader()
	{
		System.out.println("libSDL: Starting data downloader");
		class Callback implements Runnable
		{
			public MainActivity Parent;
			public void run()
			{
				System.out.println("libSDL: Removing button from startup screen and adding status text");
				if( Parent._btn != null )
				{
					Parent._layout2.removeView(Parent._btn);
					Parent._btn = null;
				}
				if( Parent._tv == null )
				{
					Parent._tv = new TextView(Parent);
					Parent._tv.setText(R.string.init);
					Parent._layout2.addView(Parent._tv);
				}

				System.out.println("libSDL: Starting downloader");
				if( Parent.downloader == null )
					Parent.downloader = new DataDownloader(Parent, Parent._tv);
			}
		}
		Callback cb = new Callback();
		cb.Parent = this;
		this.runOnUiThread(cb);
	}

	public void initSDL()
	{
		if(sdlInited)
			return;
		System.out.println("libSDL: Initializing video and SDL application");
		sdlInited = true;
		if(Globals.UseAccelerometerAsArrowKeys)
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
					WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		mGLView = new DemoGLSurfaceView(this);
		setContentView(mGLView);
		// Receive keyboard events
		mGLView.setFocusableInTouchMode(true);
		mGLView.setFocusable(true);
		mGLView.requestFocus();
	}

	@Override
	protected void onPause() {
		if( downloader != null ) {
			synchronized( downloader ) {
				downloader.setParent(null, null);
			}
		}
		if( mGLView != null )
			mGLView.onPause();
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if( mGLView != null )
			mGLView.onResume();
		else
		if( downloader != null ) {
			synchronized( downloader ) {
				downloader.setParent(this, _tv);
				if( downloader.DownloadComplete )
					initSDL();
			}
		}
	}

	@Override
	protected void onDestroy() 
	{
		if( downloader != null ) {
			synchronized( downloader ) {
				downloader.setParent(null, null);
			}
		}
		if( mGLView != null )
			mGLView.exitApp();
		super.onDestroy();
		System.exit(0);
	}

	@Override
	public boolean onKeyDown(int keyCode, final KeyEvent event) {
		// Overrides Back key to use in our app
		if( mGLView != null )
			 mGLView.nativeKey( keyCode, 1 );
		else
		if( keyCode == KeyEvent.KEYCODE_BACK && downloader != null )
		{ 
			if( downloader.DownloadFailed )
				System.exit(1);
			if( !downloader.DownloadComplete )
			 onStop();
		}
		 return true;
	}
	
	@Override
	public boolean onKeyUp(int keyCode, final KeyEvent event) {
		 if( mGLView != null )
			 mGLView.nativeKey( keyCode, 0 );
		 return true;
	}
	
	@Override
	public boolean dispatchTouchEvent(final MotionEvent ev) {
		if(mGLView != null)
			mGLView.onTouchEvent(ev);
		else if( _btn != null )
			return _btn.dispatchTouchEvent(ev);
		return true;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// Do nothing here
	}
	
	public void setText(final String t)
	{
		class Callback implements Runnable
		{
			public TextView Status;
			public String text;
			public void run()
			{
				if(Status != null)
					Status.setText(text);
			}
		}
		Callback cb = new Callback();
		cb.text = new String(t);
		cb.Status = _tv;
		this.runOnUiThread(cb);
	}

	private static DemoGLSurfaceView mGLView = null;
	private static LoadLibrary mLoadLibraryStub = null;
	private static AudioThread mAudioThread = null;
	private static DataDownloader downloader = null;
	private TextView _tv = null;
	private Button _btn = null;
	private LinearLayout _layout = null;
	private LinearLayout _layout2 = null;
	private boolean sdlInited = false;

}
