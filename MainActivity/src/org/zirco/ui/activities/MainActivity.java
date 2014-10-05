/*
 * Zirco Browser for Android
 * 
 * Copyright (C) 2010 - 2012 J. Devauchelle and contributors.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 3 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package org.zirco.ui.activities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.greendroid.QuickAction;
import org.greendroid.QuickActionGrid;
import org.greendroid.QuickActionWidget;
import org.greendroid.QuickActionWidget.OnQuickActionClickListener;
import org.zirco.R;
import org.zirco.controllers.Controller;
import org.zirco.events.EventConstants;
import org.zirco.events.EventController;
import org.zirco.events.IDownloadEventsListener;
import org.zirco.model.adapters.UrlSuggestionCursorAdapter;
import org.zirco.model.items.DownloadItem;
import org.zirco.providers.BookmarksProviderWrapper;
import org.zirco.providers.BookmarksProviderWrapper.BookmarksSource;
import org.zirco.ui.activities.preferences.PreferencesActivity;
import org.zirco.ui.components.CustomWebView;
import org.zirco.ui.components.CustomWebViewClient;
import org.zirco.ui.runnables.FaviconUpdaterRunnable;
import org.zirco.ui.runnables.HideToolbarsRunnable;
import org.zirco.ui.runnables.HistoryUpdater;
import org.zirco.utils.AnimationManager;
import org.zirco.utils.ApplicationUtils;
import org.zirco.utils.Constants;
import org.zirco.utils.UrlUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.database.Cursor;
import android.gesture.GesturePoint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.DownloadListener;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebIconDatabase;
import android.webkit.WebView;
import android.webkit.WebView.HitTestResult;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter.CursorToStringConverter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

/*import org.apache.poi.ss.usermodel.DateUtil;
 import org.apache.poi.ss.usermodel.IndexedColors;
 import org.apache.poi.ss.util.CellReference;
 import org.apache.poi.xssf.usermodel.*;*/

/**
 * The application main activity.
 */
public class MainActivity extends Activity implements IToolbarsContainer,
		OnTouchListener, IDownloadEventsListener, SensorEventListener,
		OnGestureListener {

	public static MainActivity INSTANCE = null;

	private static final int FLIP_PIXEL_THRESHOLD = 200;
	private static final int FLIP_TIME_THRESHOLD = 400;

	private static final int MENU_ADD_BOOKMARK = Menu.FIRST;
	private static final int MENU_SHOW_BOOKMARKS = Menu.FIRST + 1;
	private static final int MENU_SHOW_DOWNLOADS = Menu.FIRST + 2;
	private static final int MENU_PREFERENCES = Menu.FIRST + 3;
	private static final int MENU_EXIT = Menu.FIRST + 4;

	private static final int CONTEXT_MENU_OPEN = Menu.FIRST + 10;
	private static final int CONTEXT_MENU_OPEN_IN_NEW_TAB = Menu.FIRST + 11;
	private static final int CONTEXT_MENU_DOWNLOAD = Menu.FIRST + 12;
	private static final int CONTEXT_MENU_COPY = Menu.FIRST + 13;
	private static final int CONTEXT_MENU_SEND_MAIL = Menu.FIRST + 14;
	private static final int CONTEXT_MENU_SHARE = Menu.FIRST + 15;

	private static final int OPEN_BOOKMARKS_HISTORY_ACTIVITY = 0;
	private static final int OPEN_DOWNLOADS_ACTIVITY = 1;
	private static final int OPEN_FILE_CHOOSER_ACTIVITY = 2;

	protected static final FrameLayout.LayoutParams COVER_SCREEN_PARAMS = new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT,
			ViewGroup.LayoutParams.MATCH_PARENT);

	protected LayoutInflater mInflater = null;

	private LinearLayout mTopBar;
	private LinearLayout mBottomBar;

	private LinearLayout mFindBar;

	private ImageButton mFindPreviousButton;
	private ImageButton mFindNextButton;
	private ImageButton mFindCloseButton;

	private EditText mFindText;

	private ImageView mPreviousTabView;
	private ImageView mNextTabView;

	private ImageButton mToolsButton;
	private AutoCompleteTextView mUrlEditText;
	private ImageButton mGoButton;
	private ProgressBar mProgressBar;

	private ImageView mBubbleRightView;
	private ImageView mBubbleLeftView;

	private CustomWebView mCurrentWebView;
	private List<CustomWebView> mWebViews;

	private ImageButton mPreviousButton;
	private ImageButton mNextButton;

	boolean previousBtn = false;
	boolean nxtBtn = false;

	private ImageButton mNewTabButton;
	private ImageButton mRemoveTabButton;

	private ImageButton mQuickButton;

	private Drawable mCircularProgress;

	private boolean mUrlBarVisible;
	private boolean mToolsActionGridVisible = false;
	private boolean mFindDialogVisible = false;

	private TextWatcher mUrlTextWatcher;

	private HideToolbarsRunnable mHideToolbarsRunnable;

	private ViewFlipper mViewFlipper;

	private GestureDetector mGestureDetector;

	private SwitchTabsMethod mSwitchTabsMethod = SwitchTabsMethod.BOTH;

	private QuickActionGrid mToolsActionGrid;

	private ValueCallback<Uri> mUploadMessage;

	private OnSharedPreferenceChangeListener mPreferenceChangeListener;

	private View mCustomView;
	private Bitmap mDefaultVideoPoster = null;
	private View mVideoProgressView = null;

	private FrameLayout mFullscreenContainer;
	// private static String FILE1 = "Research1.xls";
	// private static String FILE2 = "Research2.xls";
	// private static String FILE3 = "Research3.xls";
	// private static String FILE4_SENSOR1 = "Research_sensor1.xls";
	// private static String FILE5_SENSOR2 = "Research_sensor2.xls";
	// private static String FILE6_SENSOR3 = "Research_sensor3.xls";
	private WebChromeClient.CustomViewCallback mCustomViewCallback;
	// venki
	String root = android.os.Environment.getExternalStorageDirectory()
			.toString();
	File mydir = new File(root + "/saved_texts");
	File file1 = new File(mydir, "Research1.xls");
	File file2 = new File(mydir, "Research2.xls");
	File file3 = new File(mydir, "Research3.xls");
	File file4 = new File(mydir, "Research4.xls");
	File file5 = new File(mydir, "Research5.xls");
	File file6 = new File(mydir, "Research_sensor1.xls");
	File file7 = new File(mydir, "Research_sensor2.xls");
	File file8 = new File(mydir, "Research_sensor3.xls");
	File file9 = new File(mydir, "Research_sensor4.xls");
	File file10 = new File(mydir, "Research_sensor5.xls");
	// File file1;
	// File file2;
	// File file3;
	// File file4;
	// File file5;
	// File file6;
	FileOutputStream out_stream;
	FileOutputStream out_stream1;
	FileOutputStream out_stream2;
	FileOutputStream out_stream3;
	FileOutputStream out_stream4;
	FileOutputStream out_stream5;
	FileOutputStream out_stream6;
	FileOutputStream out_stream7;
	FileOutputStream out_stream8;
	FileOutputStream out_stream9;
	private static String TAG = "MainActivity";
	private GestureDetector gestureDetector;
	View.OnTouchListener gestureListener;
	private SparseArray<PointF> mActivePointers;
	private float degrees = 0;
	private String touchDirection;
	private SensorManager mSensorManager;
	private Sensor mLight;
	float[] gData = new float[3]; // Gravity or accelerometer
	float[] mData = new float[3]; // Magnetometer
	float[] orientation = new float[3];
	float[] Rmat = new float[9];
	float[] R2 = new float[9];
	float[] Imat = new float[9];
	boolean haveGrav = false;
	boolean haveAccel = false;
	boolean haveMag = false;
	float x1, x2, y1, y2, dx = 0, dy = 0;
	String direction = "";
	static final int MIN_DISTANCE = 50;
	// @SuppressWarnings("unused")
	private static GestureDetector detector;

	/*
	 * org.apache.poi.ss.usermodel.Sheet insheet = wb.getSheetAt(0); //of the
	 * second columm Row row = insheet.getRow(rownum); //in the 11th row
	 * (arbitrary number used to reduce iterations and skip whitespace) Cell
	 * cell = row.getCell(column);
	 */
	// venki
	private enum SwitchTabsMethod {
		BUTTONS, FLING, BOTH
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// venki
		mActivePointers = new SparseArray<PointF>();
		try {
			out_stream = new FileOutputStream(file1);
			out_stream1 = new FileOutputStream(file2);
			out_stream2 = new FileOutputStream(file3);
			out_stream3 = new FileOutputStream(file4);
			out_stream4 = new FileOutputStream(file5);
			out_stream5 = new FileOutputStream(file6);
			out_stream6 = new FileOutputStream(file7);
			out_stream7 = new FileOutputStream(file8);
			out_stream8 = new FileOutputStream(file9);
			out_stream9 = new FileOutputStream(file10);
			// out_stream = openFileOutput(FILE1, Context.MODE_PRIVATE);
			// file1 = getFileStreamPath(FILE1);
			// out_stream1 = openFileOutput(FILE2, Context.MODE_PRIVATE);
			// file2 = getFileStreamPath(FILE2);
			// out_stream2 = openFileOutput(FILE3, Context.MODE_PRIVATE);
			// file3 = getFileStreamPath(FILE3);
			// out_stream3 = openFileOutput(FILE4_SENSOR1,
			// Context.MODE_PRIVATE);
			// file4 = getFileStreamPath(FILE4_SENSOR1);
			// out_stream4 = openFileOutput(FILE5_SENSOR2,
			// Context.MODE_PRIVATE);
			// file5 = getFileStreamPath(FILE5_SENSOR2);
			// out_stream5 = openFileOutput(FILE6_SENSOR3,
			// Context.MODE_PRIVATE);
			// file6 = getFileStreamPath(FILE6_SENSOR3);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e(TAG, e.getClass().getName(), e);
		}

		String str9 = "Timestamp" + '\t' + "Time in millisec" + '\t' + "Finger"
				+ '\t' + "X-Coordinate" + '\t' + "Y-Coordinate" + '\t'
				+ "Pressure" + '\t' + "Finger Size" + '\t' + "Orientation"
				+ '\t' + "ACTION" + '\t' + "touchDirection" + '\t'
				+ "Direction" + '\t' + '\t' + '\t' + "Finger" + '\t'
				+ "X-Coordinate" + '\t' + "Y-Coordinate" + '\t' + "Pressure"
				+ '\t' + "Finger Size" + '\t' + "Orientation" + '\t' + "ACTION"
				+ '\t' + "touchDirection" + '\t' + "Direction" + '\t' + '\n';

		try {
			out_stream.write(str9.getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			out_stream1.write(str9.getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			out_stream2.write(str9.getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			out_stream3.write(str9.getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			out_stream4.write(str9.getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String strsensor = "Sensor Type" + '\t' + "X" + '\t' + "Y" + '\t' + "Z"
				+ '\t' + "Time in millisec" + '\t' + "TimeStamp" + '\t' + '\n';
		try {
			out_stream5.write(strsensor.getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			out_stream6.write(strsensor.getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			out_stream7.write(strsensor.getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			out_stream8.write(strsensor.getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			out_stream9.write(strsensor.getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		detector = new GestureDetector(this);
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
		// detector= new GestureDetector(this, new MainActivity());

		// venki
		INSTANCE = this;

		Constants.initializeConstantsFromResources(this);

		Controller.getInstance().setPreferences(
				PreferenceManager.getDefaultSharedPreferences(this));

		if (Controller.getInstance().getPreferences()
				.getBoolean(Constants.PREFERENCES_SHOW_FULL_SCREEN, false)) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}

		if (Controller
				.getInstance()
				.getPreferences()
				.getBoolean(Constants.PREFERENCES_GENERAL_HIDE_TITLE_BARS, true)) {
			requestWindowFeature(Window.FEATURE_NO_TITLE);
		}

		setProgressBarVisibility(true);

		setContentView(R.layout.main);

		mCircularProgress = getResources().getDrawable(R.drawable.spinner);

		EventController.getInstance().addDownloadListener(this);

		mHideToolbarsRunnable = null;

		mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		buildComponents();

		mViewFlipper.removeAllViews();

		updateSwitchTabsMethod();
		updateBookmarksDatabaseSource();

		registerPreferenceChangeListener();

		Intent i = getIntent();
		if (i.getData() != null && !i.getDataString().startsWith("touchtest-")) {
			// App first launch from another app.
			addTab(false);
			navigateToUrl(i.getDataString());
		} else {
			// Normal start.
			int currentVersionCode = ApplicationUtils
					.getApplicationVersionCode(this);
			int savedVersionCode = PreferenceManager
					.getDefaultSharedPreferences(this).getInt(
							Constants.PREFERENCES_LAST_VERSION_CODE, -1);

			// If currentVersionCode and savedVersionCode are different, the
			// application has been updated.
			if (currentVersionCode != savedVersionCode) {
				// Save current version code.
				Editor editor = PreferenceManager.getDefaultSharedPreferences(
						this).edit();
				editor.putInt(Constants.PREFERENCES_LAST_VERSION_CODE,
						currentVersionCode);
				editor.commit();

				// Display changelog dialog.
				Intent changelogIntent = new Intent(this,
						ChangelogActivity.class);
				startActivity(changelogIntent);
			}

			boolean lastPageRestored = false;
			if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
					Constants.PREFERENCES_BROWSER_RESTORE_LAST_PAGE, false)) {
				if (savedInstanceState != null) {
					String savedUrl = savedInstanceState
							.getString(Constants.EXTRA_SAVED_URL);
					if (savedUrl != null) {
						addTab(false);
						navigateToUrl(savedUrl);
						lastPageRestored = true;
					}
				}
			}

			if (!lastPageRestored) {
				addTab(true);
			}
		}

		initializeWebIconDatabase();

		startToolbarsHideRunnable();

	}

	/**
	 * Initialize the Web icons database.
	 */
	private void initializeWebIconDatabase() {

		final WebIconDatabase db = WebIconDatabase.getInstance();
		db.open(getDir("icons", 0).getPath());
	}

	@Override
	protected void onDestroy() {
		WebIconDatabase.getInstance().close();

		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
				Constants.PREFERENCES_PRIVACY_CLEAR_CACHE_ON_EXIT, false)) {
			mCurrentWebView.clearCache(true);
		}

		EventController.getInstance().removeDownloadListener(this);

		PreferenceManager.getDefaultSharedPreferences(this)
				.unregisterOnSharedPreferenceChangeListener(
						mPreferenceChangeListener);

		super.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString(Constants.EXTRA_SAVED_URL, mCurrentWebView.getUrl());
		super.onSaveInstanceState(outState);
	}

	/**
	 * Handle url request from external apps.
	 * 
	 * @param intent
	 *            The intent.
	 */
	@Override
	protected void onNewIntent(Intent intent) {
		if (intent.getData() != null
				&& !intent.getDataString().startsWith("touchtest-")) {
			addTab(false);
			navigateToUrl(intent.getDataString());
		} else {
			addTab(true);
		}

		setIntent(intent);

		super.onNewIntent(intent);
	}

	/**
	 * Restart the application.
	 */
	public void restartApplication() {
		PendingIntent intent = PendingIntent.getActivity(this.getBaseContext(),
				0, new Intent(getIntent()), getIntent().getFlags());
		AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 2000, intent);
		System.exit(2);
	}

	/**
	 * Create main UI.
	 */
	private void buildComponents() {

		mToolsActionGrid = new QuickActionGrid(this);
		mToolsActionGrid.addQuickAction(new QuickAction(this,
				R.drawable.ic_btn_home, R.string.QuickAction_Home));
		mToolsActionGrid.addQuickAction(new QuickAction(this,
				R.drawable.ic_btn_share, R.string.QuickAction_Share));
		mToolsActionGrid.addQuickAction(new QuickAction(this,
				R.drawable.ic_btn_find, R.string.QuickAction_Find));
		mToolsActionGrid.addQuickAction(new QuickAction(this,
				R.drawable.ic_btn_select, R.string.QuickAction_SelectText));
		mToolsActionGrid
				.addQuickAction(new QuickAction(this,
						R.drawable.ic_btn_mobile_view,
						R.string.QuickAction_MobileView));

		mToolsActionGrid
				.setOnQuickActionClickListener(new OnQuickActionClickListener() {
					@Override
					public void onQuickActionClicked(QuickActionWidget widget,
							int position) {
						switch (position) {
						case 0:
							navigateToHome();
							break;
						case 1:
							ApplicationUtils.sharePage(MainActivity.this,
									mCurrentWebView.getTitle(),
									mCurrentWebView.getUrl());
							break;
						case 2:
							// Somewhat dirty hack: when the find dialog was
							// shown from a QuickAction,
							// the soft keyboard did not show... Hack is to wait
							// a little before showing
							// the file dialog through a thread.
							startShowFindDialogRunnable();
							break;
						case 3:
							swithToSelectAndCopyTextMode();
							break;
						case 4:
							String currentUrl = mUrlEditText.getText()
									.toString();

							// Do not reload mobile view if already on it.
							if (!currentUrl
									.startsWith(Constants.URL_GOOGLE_MOBILE_VIEW_NO_FORMAT)) {
								String url = String.format(
										Constants.URL_GOOGLE_MOBILE_VIEW,
										mUrlEditText.getText().toString());
								navigateToUrl(url);
							}
							break;
						}
					}
				});

		mToolsActionGrid
				.setOnDismissListener(new PopupWindow.OnDismissListener() {
					@Override
					public void onDismiss() {
						mToolsActionGridVisible = false;
						startToolbarsHideRunnable();
					}
				});

		mGestureDetector = new GestureDetector(this, new GestureListener());

		mUrlBarVisible = true;

		mWebViews = new ArrayList<CustomWebView>();
		Controller.getInstance().setWebViewList(mWebViews);

		mBubbleRightView = (ImageView) findViewById(R.id.BubbleRightView);
		mBubbleRightView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setToolbarsVisibility(true);
			}
		});
		mBubbleRightView.setVisibility(View.GONE);

		mBubbleLeftView = (ImageView) findViewById(R.id.BubbleLeftView);
		mBubbleLeftView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setToolbarsVisibility(true);
			}
		});
		mBubbleLeftView.setVisibility(View.GONE);

		mViewFlipper = (ViewFlipper) findViewById(R.id.ViewFlipper);

		mTopBar = (LinearLayout) findViewById(R.id.BarLayout);
		mTopBar.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Dummy event to steel it from the WebView, in case of clicking
				// between the buttons.
			}
		});

		mBottomBar = (LinearLayout) findViewById(R.id.BottomBarLayout);
		mBottomBar.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Dummy event to steel it from the WebView, in case of clicking
				// between the buttons.
			}
		});

		mFindBar = (LinearLayout) findViewById(R.id.findControls);
		mFindBar.setVisibility(View.GONE);

		mPreviousTabView = (ImageView) findViewById(R.id.PreviousTabView);
		mPreviousTabView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showPreviousTab(true);
			}
		});
		mPreviousTabView.setVisibility(View.GONE);

		mNextTabView = (ImageView) findViewById(R.id.NextTabView);
		mNextTabView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showNextTab(true);
			}
		});
		mNextTabView.setVisibility(View.GONE);

		String[] from = new String[] {
				UrlSuggestionCursorAdapter.URL_SUGGESTION_TITLE,
				UrlSuggestionCursorAdapter.URL_SUGGESTION_URL };
		int[] to = new int[] { R.id.AutocompleteTitle, R.id.AutocompleteUrl };

		UrlSuggestionCursorAdapter adapter = new UrlSuggestionCursorAdapter(
				this, R.layout.url_autocomplete_line, null, from, to);

		adapter.setCursorToStringConverter(new CursorToStringConverter() {
			@Override
			public CharSequence convertToString(Cursor cursor) {
				String aColumnString = cursor.getString(cursor
						.getColumnIndex(UrlSuggestionCursorAdapter.URL_SUGGESTION_URL));
				return aColumnString;
			}
		});

		adapter.setFilterQueryProvider(new FilterQueryProvider() {
			@Override
			public Cursor runQuery(CharSequence constraint) {
				if ((constraint != null) && (constraint.length() > 0)) {
					return BookmarksProviderWrapper.getUrlSuggestions(
							getContentResolver(),
							constraint.toString(),
							PreferenceManager.getDefaultSharedPreferences(
									MainActivity.this).getBoolean(
									Constants.PREFERENCE_USE_WEAVE, false));
				} else {
					return BookmarksProviderWrapper.getUrlSuggestions(
							getContentResolver(),
							null,
							PreferenceManager.getDefaultSharedPreferences(
									MainActivity.this).getBoolean(
									Constants.PREFERENCE_USE_WEAVE, false));
				}
			}
		});

		mUrlEditText = (AutoCompleteTextView) findViewById(R.id.UrlText);
		mUrlEditText.setThreshold(1);
		mUrlEditText.setAdapter(adapter);

		mUrlEditText.setOnKeyListener(new View.OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_ENTER) {
					navigateToUrl();
					return true;
				}

				return false;
			}

		});

		mUrlTextWatcher = new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2,
					int arg3) {
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,
					int arg2, int arg3) {
			}

			@Override
			public void afterTextChanged(Editable arg0) {
				updateGoButton();
			}
		};

		mUrlEditText.addTextChangedListener(mUrlTextWatcher);

		mUrlEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				// Select all when focus gained.
				if (hasFocus) {
					mUrlEditText.setSelection(0, mUrlEditText.getText()
							.length());
				}
			}
		});

		mUrlEditText.setCompoundDrawablePadding(5);

		mGoButton = (ImageButton) findViewById(R.id.GoBtn);
		mGoButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {

				if (mCurrentWebView.isLoading()) {
					mCurrentWebView.stopLoading();
				} else if (!mCurrentWebView.isSameUrl(mUrlEditText.getText()
						.toString())) {
					navigateToUrl();
				} else {
					mCurrentWebView.reload();
				}
			}
		});

		mToolsButton = (ImageButton) findViewById(R.id.ToolsBtn);
		mToolsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mToolsActionGridVisible = true;
				mToolsActionGrid.show(v);
			}
		});

		mProgressBar = (ProgressBar) findViewById(R.id.WebViewProgress);
		mProgressBar.setMax(100);

		mPreviousButton = (ImageButton) findViewById(R.id.PreviousBtn);
		mNextButton = (ImageButton) findViewById(R.id.NextBtn);

		mPreviousButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				navigatePrevious();
				previousBtn = true;
			}
		});

		mNextButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				navigateNext();
				nxtBtn = true;
			}
		});

		mNewTabButton = (ImageButton) findViewById(R.id.NewTabBtn);
		mNewTabButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				addTab(true);
			}
		});

		mRemoveTabButton = (ImageButton) findViewById(R.id.RemoveTabBtn);
		mRemoveTabButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				if (mViewFlipper.getChildCount() == 1
						&& !mCurrentWebView.getUrl().equals(
								Constants.URL_ABOUT_START)) {
					navigateToHome();
					updateUI();
					updatePreviousNextTabViewsVisibility();
				} else
					removeCurrentTab();
			}
		});

		mQuickButton = (ImageButton) findViewById(R.id.QuickBtn);
		mQuickButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				onQuickButton();
			}
		});

		mFindPreviousButton = (ImageButton) findViewById(R.id.find_previous);
		mFindPreviousButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mCurrentWebView.findNext(false);
				hideKeyboardFromFindDialog();
			}
		});

		mFindNextButton = (ImageButton) findViewById(R.id.find_next);
		mFindNextButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mCurrentWebView.findNext(true);
				hideKeyboardFromFindDialog();
			}
		});

		mFindCloseButton = (ImageButton) findViewById(R.id.find_close);
		mFindCloseButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				closeFindDialog();
			}
		});

		mFindText = (EditText) findViewById(R.id.find_value);
		mFindText.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				doFind();
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});

	}

	private void registerPreferenceChangeListener() {
		mPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
			@Override
			public void onSharedPreferenceChanged(
					SharedPreferences sharedPreferences, String key) {
				if (key.equals(Constants.PREFERENCE_BOOKMARKS_DATABASE)) {
					updateBookmarksDatabaseSource();
				}
			}
		};

		PreferenceManager.getDefaultSharedPreferences(this)
				.registerOnSharedPreferenceChangeListener(
						mPreferenceChangeListener);
	}

	/**
	 * Apply preferences to the current UI objects.
	 */
	public void applyPreferences() {
		// To update to Bubble position.
		setToolbarsVisibility(false);

		updateSwitchTabsMethod();

		for (CustomWebView view : mWebViews) {
			view.initializeOptions();
		}
	}

	private void updateSwitchTabsMethod() {
		String method = PreferenceManager.getDefaultSharedPreferences(this)
				.getString(Constants.PREFERENCES_GENERAL_SWITCH_TABS_METHOD,
						"buttons");

		if (method.equals("buttons")) {
			mSwitchTabsMethod = SwitchTabsMethod.BUTTONS;
		} else if (method.equals("fling")) {
			mSwitchTabsMethod = SwitchTabsMethod.FLING;
		} else if (method.equals("both")) {
			mSwitchTabsMethod = SwitchTabsMethod.BOTH;
		} else {
			mSwitchTabsMethod = SwitchTabsMethod.BUTTONS;
		}
	}

	private void updateBookmarksDatabaseSource() {
		String source = PreferenceManager.getDefaultSharedPreferences(this)
				.getString(Constants.PREFERENCE_BOOKMARKS_DATABASE, "STOCK");

		if (source.equals("STOCK")) {
			BookmarksProviderWrapper.setBookmarksSource(BookmarksSource.STOCK);
		} else if (source.equals("INTERNAL")) {
			BookmarksProviderWrapper
					.setBookmarksSource(BookmarksSource.INTERNAL);
		}
	}

	private void setStatusBarVisibility(boolean visible) {
		int flag = visible ? 0 : WindowManager.LayoutParams.FLAG_FULLSCREEN;
		getWindow().setFlags(flag, WindowManager.LayoutParams.FLAG_FULLSCREEN);
	}

	/**
	 * Initialize a newly created WebView.
	 */
	private void initializeCurrentWebView() {

		mCurrentWebView.setWebViewClient(new CustomWebViewClient(this));
		mCurrentWebView.setOnTouchListener(this);

		mCurrentWebView.setOnTouchListener(new View.OnTouchListener() {

			@SuppressLint("NewApi")
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				int pointerIndex = event.getActionIndex();
				int pointerId = event.getPointerId(pointerIndex);
				int maskedAction = event.getActionMasked();
				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
				String currentDateandTime = sdf.format(new Date());
				int tab = mViewFlipper.getDisplayedChild() + 1;
				Log.d("WTF", String.valueOf(tab));
				Calendar c = Calendar.getInstance();
				switch (maskedAction) {
				case MotionEvent.ACTION_DOWN: {
					// EventData eventData = new EventData();
					// handler.postDelayed(CaptureEventsActivity.this.mLongPressed,
					// 1000);
					GesturePoint gp = new GesturePoint(event.getX(), event
							.getY(), System.currentTimeMillis());
					// aa.add(gp);
					x1 = event.getX();
					y1 = event.getY();
					int index = event.getActionIndex();
					int id = event.getPointerId(index);
					String finger = "finger-" + id;
					int orientation = getResources().getConfiguration().orientation;

					// bcn.path.moveTo(event.getX(), event.getY());

					touchDirection = getTouchDirection(event.getX(),
							event.getY());
					String strD = '\n' + currentDateandTime + '\t'
							+ c.getTimeInMillis() + '\t' + '\t'
							+ String.valueOf((int) event.getX()) + '\t'
							+ String.valueOf((int) event.getY()) + '\t'
							+ event.getPressure() + '\t' + event.getSize()
							+ '\t' + orientation + '\t' + "ACTION_DOWN" + '\t'
							+ touchDirection + '\t' + '\t' + '\t' + '\t' + '\t';
					if (tab == 1 || tab == 6)

					{
						try {
							out_stream.write(strD.getBytes());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}
					if (tab == 2) {
						try {
							out_stream1.write(strD.getBytes());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					if (tab == 3) {
						try {
							out_stream2.write(strD.getBytes());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					if (tab == 4) {
						try {
							out_stream3.write(strD.getBytes());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					if (tab == 5) {
						try {
							out_stream4.write(strD.getBytes());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					break;
				}
				case MotionEvent.ACTION_MOVE: {
					int j = 0;
					Log.d("before pointer count",
							String.valueOf(event.getPointerCount()));

					if (event.getPointerCount() > 1) {
						Log.d("after pointer count",
								String.valueOf(event.getPointerCount()));
						for (int i = 0; i < event.getPointerCount(); i++) {
							// PointF point = mActivePointers.get(event
							// .getPointerId(i));
							// if (point != null) {
							Log.d("ActionMove", String.valueOf(i));
							String x = "";
							// point.x=event.getX(i);
							// point.y=event.getX(i);
							// if(i==1)
							Log.d("ActionMove", "Finger");
							x = "finger-" + i;
							touchDirection = getTouchDirection(event.getX(i),
									event.getY(i));
							int orientation = getResources().getConfiguration().orientation;
							String strM = "";
							if (i == 0) {

								strM = '\n' + currentDateandTime + '\t'
										+ c.getTimeInMillis() + '\t' + x + '\t'
										+ String.valueOf((int) event.getX(i))
										+ '\t'
										+ String.valueOf((int) event.getY(i))
										+ '\t' + event.getPressure(i) + '\t'
										+ event.getSize(i) + '\t' + orientation
										+ '\t' + "ACTION_MOVE" + '\t'
										+ touchDirection + '\t' + '\t' + '\t'
										+ '\t';
							} else {
								strM = x + '\t'
										+ String.valueOf((int) event.getX(i))
										+ '\t'
										+ String.valueOf((int) event.getY(i))
										+ '\t' + event.getPressure(i) + '\t'
										+ event.getSize(i) + '\t' + orientation
										+ '\t' + "ACTION_MOVE" + '\t'
										+ touchDirection + '\t' + '\t' + '\t'
										+ '\t';
							}
							// else
							// {
							/*
							 * for(int k =0 ; k< insheet.getLastRowNum(); k++){
							 * //do stuff }
							 */
							// if(j==0)
							// strM =
							// '\t'+'\t'+'\t'+'\t'+'\t'+'\t'+String.valueOf((int)event.getX(i))
							// +'\t'+ String.valueOf((int)event.getY(i))
							// +'\t'+ orientation+'\t'
							// +event.getPressure(i)+'\t'+"ACTION_MOVE"+'\t'+event.getSize(i)+'\t'+touchDirection+'\t'+System.currentTimeMillis()+'\t'+x+'\n';
							// else
							// strM ='\t'+'\t'+'\t'+'\t'+'\t'+'\t'+'\t'+
							// '\t'+'\t'+'\t'+'\t'+'\t'+'\t'+'\t'+'\t'+'\t'+'\t'+'\t'+'\t'+'\t'+'\t'+String.valueOf((int)event.getX(i))
							// +'\t'+ String.valueOf((int)event.getY(i))
							// +'\t'+ orientation+'\t'
							// +event.getPressure(i)+'\t'+"ACTION_MOVE"+'\t'+event.getSize(i)+'\t'+touchDirection+'\t'+currentDateandTime+'\t'+x+'\n';

							// j++;
							// }

							if (tab == 1 || tab == 6)

							{
								try {
									out_stream.write(strM.getBytes());
								} catch (IOException e) {
									// TODO Auto-generated catch
									// block
									e.printStackTrace();
								}

							}
							if (tab == 2) {
								try {
									out_stream1.write(strM.getBytes());
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}

							if (tab == 3) {
								try {
									out_stream2.write(strM.getBytes());
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
							if (tab == 4) {
								try {
									out_stream3.write(strM.getBytes());
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}

							if (tab == 5) {
								try {
									out_stream4.write(strM.getBytes());
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}

							// if (i + 1 < event.getPointerCount())
							// strM = " ";
							// }
						}
					} else {
						// handler.removeCallbacks(CaptureEventsActivity.this.mLongPressed);
						// bcn.path.lineTo(event.getX(), event.getY());
						touchDirection = getTouchDirection(event.getX(),
								event.getY());
						int orientation = getResources().getConfiguration().orientation;
						String strM = '\n' + currentDateandTime + '\t'
								+ c.getTimeInMillis() + '\t' + '\t'
								+ String.valueOf((int) event.getX()) + '\t'
								+ String.valueOf((int) event.getY()) + '\t'
								+ event.getPressure() + '\t' + event.getSize()
								+ '\t' + orientation + '\t' + "ACTION_MOVE"
								+ '\t' + touchDirection + '\t' + '\t' + '\t'
								+ '\t';

						if (tab == 1 || tab == 6)

						{
							try {
								out_stream.write(strM.getBytes());
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

						}
						if (tab == 2) {
							try {
								out_stream1.write(strM.getBytes());
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}

						if (tab == 3) {
							try {
								out_stream2.write(strM.getBytes());
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						if (tab == 4) {
							try {
								out_stream3.write(strM.getBytes());
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}

						if (tab == 5) {
							try {
								out_stream4.write(strM.getBytes());
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
					Log.d("Action move last","WTF Error");
					break;
				}
				

				case MotionEvent.ACTION_POINTER_DOWN: {

					// PointF f = new PointF();
					// f.x = event.getX(pointerIndex);
					// f.y = event.getY(pointerIndex);
					// mActivePointers.put(pointerId, f);
					// PointF f = new PointF();

					GesturePoint gp = new GesturePoint(event.getX(), event
							.getY(), System.currentTimeMillis());
					// aa.add(gp);
					// f.x = event.getX(pointerIndex);
					// f.y = event.getY(pointerIndex);

					// bcn.path.moveTo(event.getX(), event.getY());
					// for (int i = 0; i < event.getPointerCount(); i++) {

					// Log.d("Action Pointer Down", String.valueOf(i));
					String x = "";
					int index = event.getActionIndex();
					int id = event.getPointerId(index);
					Log.d("Action Pointer Down", "Finger");
					x = "finger-" + id;
					int orientation = getResources().getConfiguration().orientation;
					touchDirection = getTouchDirection(event.getX(id),
							event.getY(id));
					String strA = "";
					// if(i==0)
					// {
					// +'\t'+'\t'+'\t'+'\t'+'\t'+'\t'+'\t'+'\t'+'\t'+'\t'+'\t'+'\t'
					strA = '\n' + currentDateandTime + '\t'
							+ c.getTimeInMillis() + '\t' + x + '\t'
							+ String.valueOf((int) event.getX(id)) + '\t'
							+ String.valueOf((int) event.getY(id)) + '\t'
							+ event.getPressure(id) + '\t' + event.getSize(id)
							+ '\t' + orientation + '\t' + "ACTION_POINTER_DOWN"
							+ '\t' + touchDirection + '\t' + '\t' + '\t' + '\t';
					// }
					// else
					// {
					// strA = String.valueOf(event.getX(i)) + '\t'
					// + String.valueOf((event.getY(i))) + '\t' + orientation
					// + '\t' + event.getPressure(i) + '\t'
					// + "ACTION_POINTER_DOWN" + '\t' + event.getSize(i)
					// + '\t' + touchDirection + '\t' + currentDateandTime
					// + '\t' + c.getTimeInMillis() + '\t'+x+'\t' + '\t' + '\t'+
					// '\t';
					// }
					// mActivePointers.put(pointerId, f);
					if (tab == 1 || tab == 6)

					{
						try {
							out_stream.write(strA.getBytes());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}
					if (tab == 2) {
						try {
							out_stream1.write(strA.getBytes());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					if (tab == 3) {
						try {
							out_stream2.write(strA.getBytes());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					if (tab == 4) {
						try {
							out_stream3.write(strA.getBytes());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					if (tab == 5) {
						try {
							out_stream4.write(strA.getBytes());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					/*
					 * for (int size = event.getPointerCount(), i = 0; i < size;
					 * i++) { PointF point = mActivePointers.get(event
					 * .getPointerId(i)); if (point != null) {
					 * 
					 * String x = ""; point.x=event.getX(i);
					 * point.y=event.getX(i); // if(i==1) x = "finger-" + i;
					 * touchDirection = getTouchDirection( event.getX(i),
					 * event.getY(i)); int orientation = getResources()
					 * .getConfiguration().orientation; String strM = ""; //
					 * if(i==0) // { strM = '\n' + String.valueOf((int)
					 * event.getX(i)) + '\t' + String.valueOf((int)
					 * event.getY(i)) + '\t' + orientation + '\t' +
					 * event.getPressure(i) + '\t' + "ACTION_POINTER_DOWN" +
					 * '\t' + event.getSize(i) + '\t' + touchDirection + '\t' +
					 * currentDateandTime + '\t' + c.getTimeInMillis() + '\t' +
					 * x;
					 * 
					 * try { out_stream.write(strM .getBytes()); } catch
					 * (IOException e) { // TODO Auto-generated catch // block
					 * e.printStackTrace(); }
					 * 
					 * 
					 * } }
					 */

					// }
					break;
				}
				case MotionEvent.ACTION_POINTER_UP: {
					// PointF f = new PointF();
					// handler.postDelayed(CaptureEventsActivity.this.mLongPressed,
					// 1000);
					GesturePoint gp = new GesturePoint(event.getX(), event
							.getY(), System.currentTimeMillis());
					// aa.add(gp);

					/*
					 * = event.getX(); y2 = event.getY(); dx = x1 - x2; dy = y1
					 * - y2; if (Math.abs(dx) > MIN_DISTANCE) { if (dx < 0)
					 * direction = "LeftToRight"; if (dx > 0) direction =
					 * "RightToLeft"; } if (Math.abs(dy) > MIN_DISTANCE) { if
					 * (dy < 0) direction = direction + "TopToBottom"; if (dy >
					 * 0) direction = direction + "BottomToTop"; }
					 */

					// f.x = event.getX(pointerIndex);
					// f.y = event.getY(pointerIndex);
					// mActivePointers.put(pointerId, f);
					// bcn.path.moveTo(event.getX(), event.getY());
					// for (int i = 0; i < event.getPointerCount(); i++) {

					// Log.d("Action Pinter Up", String.valueOf(i));
					String x = "";
					int index = event.getActionIndex();
					int id = event.getPointerId(index);

					Log.d("Actionpointer up", "Finger");
					x = "finger-" + id;
					int orientation = getResources().getConfiguration().orientation;
					touchDirection = getTouchDirection(event.getX(id),
							event.getY(id));
					String strU = "";
					// if(i==0)
					// {
					// +'\t'+'\t'+'\t'+'\t'+'\t'+'\t'+'\t'+'\t'+'\t'+'\t'+'\t'+'\t'
					strU = '\n' + currentDateandTime + '\t'
							+ c.getTimeInMillis() + '\t' + x + '\t'
							+ String.valueOf((int) event.getX(id)) + '\t'
							+ String.valueOf((int) event.getY(id)) + '\t'
							+ event.getPressure(id) + '\t' + event.getSize(id)
							+ '\t' + orientation + '\t' + "ACTION_POINTER_UP"
							+ '\t' + touchDirection + '\t' + '\t' + '\t' + '\t';
					// }
					// else
					// {
					// strU = String.valueOf((int) event.getX(i))
					// + '\t' + String.valueOf((int) event.getY(i)) + '\t'
					// + orientation + '\t' + event.getPressure(i) + '\t'
					// + "ACTION_POINTER_UP" + '\t' + event.getSize(i)
					// + '\t' + touchDirection + '\t' + currentDateandTime
					// + '\t' + c.getTimeInMillis() +'\t'+x+ '\t' + '\t'
					// + '\t' + '\t' + '\t' + '\t';
					// }

					if (tab == 1 || tab == 6)

					{
						try {
							out_stream.write(strU.getBytes());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}

					if (tab == 2) {
						try {
							out_stream1.write(strU.getBytes());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					if (tab == 3) {
						try {
							out_stream2.write(strU.getBytes());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					if (tab == 4) {
						try {
							out_stream3.write(strU.getBytes());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					if (tab == 5) {
						try {
							out_stream4.write(strU.getBytes());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					Log.d("Finger up done", "at last");

					break;
				}

				case MotionEvent.ACTION_UP: { // handler.removeCallbacks(CaptureEventsActivity.this.mLongPressed);
					GesturePoint gp = new GesturePoint(event.getX(), event
							.getY(), System.currentTimeMillis());
					// aa.add(gp);
					GesturePoint gp1 = new GesturePoint(event.getAction(),
							event.getPressure(), event.getAction());
					// aa.add(gp1);
					// GestureStroke gs = new GestureStroke(aa);

					x2 = event.getX();
					y2 = event.getY();
					dx = x1 - x2;
					dy = y1 - y2;
					if (Math.abs(dx) > MIN_DISTANCE) {
						if (dx < 0)
							direction = "LeftToRight";
						if (dx > 0)
							direction = "RightToLeft";
					}
					if (Math.abs(dy) > MIN_DISTANCE) {
						if (dy < 0)
							direction = direction + "TopToBottom";
						if (dy > 0)
							direction = direction + "BottomToTop";
					}
					Log.d("Action up", "Entered");

					int orientation = getResources().getConfiguration().orientation;
					touchDirection = getTouchDirection(event.getX(),
							event.getY());

					String strU = '\n' + currentDateandTime + '\t'
							+ c.getTimeInMillis() + '\t' + '\t'
							+ String.valueOf((int) event.getX()) + '\t'
							+ String.valueOf((int) event.getY()) + '\t'
							+ event.getPressure() + '\t' + event.getSize()
							+ '\t' + orientation + '\t' + "ACTION_UP" + '\t'
							+ touchDirection + '\t' + direction + '\t' + '\t'
							+ '\t' + '\t';

					if (tab == 1 || tab == 6)

					{
						try {
							out_stream.write(strU.getBytes());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}
					if (tab == 2) {
						try {
							out_stream1.write(strU.getBytes());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					if (tab == 3) {
						try {
							out_stream2.write(strU.getBytes());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					if (tab == 4) {
						try {
							out_stream3.write(strU.getBytes());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					if (tab == 5) {
						try {
							out_stream4.write(strU.getBytes());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					direction = "";
					// aa.clear();
					break;
				}

					

				// case MotionEvent.ACTION_CANCEL: {
				// mActivePointers.remove(pointerId);
				// break;
				// }
				default:
					/*
					 * String x =String.valueOf(bcn.getX())+'\n'; try {
					 * out_stream.write(x.getBytes()); } catch (IOException e) {
					 * // TODO Auto-generated catch block e.printStackTrace(); }
					 */
					return true;
				}

				mCurrentWebView.invalidate();
				return true;
			}
		});

		mCurrentWebView
				.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {

					@Override
					public void onCreateContextMenu(ContextMenu menu, View v,
							ContextMenuInfo menuInfo) {
						HitTestResult result = ((WebView) v).getHitTestResult();

						int resultType = result.getType();
						if ((resultType == HitTestResult.ANCHOR_TYPE)
								|| (resultType == HitTestResult.IMAGE_ANCHOR_TYPE)
								|| (resultType == HitTestResult.SRC_ANCHOR_TYPE)
								|| (resultType == HitTestResult.SRC_IMAGE_ANCHOR_TYPE)) {

							Intent i = new Intent();
							i.putExtra(Constants.EXTRA_ID_URL,
									result.getExtra());

							MenuItem item = menu.add(0, CONTEXT_MENU_OPEN, 0,
									R.string.Main_MenuOpen);
							item.setIntent(i);

							item = menu.add(0, CONTEXT_MENU_OPEN_IN_NEW_TAB, 0,
									R.string.Main_MenuOpenNewTab);
							item.setIntent(i);

							item = menu.add(0, CONTEXT_MENU_COPY, 0,
									R.string.Main_MenuCopyLinkUrl);
							item.setIntent(i);

							item = menu.add(0, CONTEXT_MENU_DOWNLOAD, 0,
									R.string.Main_MenuDownload);
							item.setIntent(i);

							item = menu.add(0, CONTEXT_MENU_SHARE, 0,
									R.string.Main_MenuShareLinkUrl);
							item.setIntent(i);

							menu.setHeaderTitle(result.getExtra());
						} else if (resultType == HitTestResult.IMAGE_TYPE) {
							Intent i = new Intent();
							i.putExtra(Constants.EXTRA_ID_URL,
									result.getExtra());

							MenuItem item = menu.add(0, CONTEXT_MENU_OPEN, 0,
									R.string.Main_MenuViewImage);
							item.setIntent(i);

							item = menu.add(0, CONTEXT_MENU_COPY, 0,
									R.string.Main_MenuCopyImageUrl);
							item.setIntent(i);

							item = menu.add(0, CONTEXT_MENU_DOWNLOAD, 0,
									R.string.Main_MenuDownloadImage);
							item.setIntent(i);

							item = menu.add(0, CONTEXT_MENU_SHARE, 0,
									R.string.Main_MenuShareImageUrl);
							item.setIntent(i);

							menu.setHeaderTitle(result.getExtra());

						} else if (resultType == HitTestResult.EMAIL_TYPE) {

							Intent sendMail = new Intent(Intent.ACTION_VIEW,
									Uri.parse(WebView.SCHEME_MAILTO
											+ result.getExtra()));

							MenuItem item = menu.add(0, CONTEXT_MENU_SEND_MAIL,
									0, R.string.Main_MenuSendEmail);
							item.setIntent(sendMail);

							Intent i = new Intent();
							i.putExtra(Constants.EXTRA_ID_URL,
									result.getExtra());

							item = menu.add(0, CONTEXT_MENU_COPY, 0,
									R.string.Main_MenuCopyEmailUrl);
							item.setIntent(i);

							item = menu.add(0, CONTEXT_MENU_SHARE, 0,
									R.string.Main_MenuShareEmailUrl);
							item.setIntent(i);

							menu.setHeaderTitle(result.getExtra());
						}
					}

				});

		mCurrentWebView.setDownloadListener(new DownloadListener() {

			@Override
			public void onDownloadStart(String url, String userAgent,
					String contentDisposition, String mimetype,
					long contentLength) {
				doDownloadStart(url, userAgent, contentDisposition, mimetype,
						contentLength);
			}

		});

		final Activity activity = this;
		mCurrentWebView.setWebChromeClient(new WebChromeClient() {

			@SuppressWarnings("unused")
			// This is an undocumented method, it _is_ used, whatever Eclipse
			// may think :)
			// Used to show a file chooser dialog.
			public void openFileChooser(ValueCallback<Uri> uploadMsg) {
				mUploadMessage = uploadMsg;
				Intent i = new Intent(Intent.ACTION_GET_CONTENT);
				i.addCategory(Intent.CATEGORY_OPENABLE);
				i.setType("*/*");
				MainActivity.this.startActivityForResult(Intent.createChooser(
						i, MainActivity.this
								.getString(R.string.Main_FileChooserPrompt)),
						OPEN_FILE_CHOOSER_ACTIVITY);
			}

			@SuppressWarnings("unused")
			// This is an undocumented method, it _is_ used, whatever Eclipse
			// may think :)
			// Used to show a file chooser dialog.
			public void openFileChooser(ValueCallback<Uri> uploadMsg,
					String acceptType) {
				mUploadMessage = uploadMsg;
				Intent i = new Intent(Intent.ACTION_GET_CONTENT);
				i.addCategory(Intent.CATEGORY_OPENABLE);
				i.setType("*/*");
				MainActivity.this.startActivityForResult(Intent.createChooser(
						i, MainActivity.this
								.getString(R.string.Main_FileChooserPrompt)),
						OPEN_FILE_CHOOSER_ACTIVITY);
			}

			@Override
			public Bitmap getDefaultVideoPoster() {
				if (mDefaultVideoPoster == null) {
					mDefaultVideoPoster = BitmapFactory.decodeResource(
							MainActivity.this.getResources(),
							R.drawable.default_video_poster);
				}

				return mDefaultVideoPoster;
			}

			@Override
			public View getVideoLoadingProgressView() {
				if (mVideoProgressView == null) {
					LayoutInflater inflater = LayoutInflater
							.from(MainActivity.this);
					mVideoProgressView = inflater.inflate(
							R.layout.video_loading_progress, null);
				}

				return mVideoProgressView;
			}

			public void onShowCustomView(View view,
					WebChromeClient.CustomViewCallback callback) {
				showCustomView(view, callback);
			}

			@Override
			public void onHideCustomView() {
				hideCustomView();
			}

			// @Override
			// public void onShowCustomView(View view, CustomViewCallback
			// callback) {
			// super.onShowCustomView(view, callback);
			//
			// if (view instanceof FrameLayout) {
			// mCustomViewContainer = (FrameLayout) view;
			// mCustomViewCallback = callback;
			//
			// mContentView = (LinearLayout) findViewById(R.id.MainContainer);
			//
			// if (mCustomViewContainer.getFocusedChild() instanceof VideoView)
			// {
			// mCustomVideoView = (VideoView)
			// mCustomViewContainer.getFocusedChild();
			// // frame.removeView(video);
			// mContentView.setVisibility(View.GONE);
			// mCustomViewContainer.setVisibility(View.VISIBLE);
			//
			// setContentView(mCustomViewContainer);
			// //mCustomViewContainer.bringToFront();
			//
			// mCustomVideoView.setOnCompletionListener(new
			// OnCompletionListener() {
			// @Override
			// public void onCompletion(MediaPlayer mp) {
			// mp.stop();
			// onHideCustomView();
			// }
			// });
			//
			// mCustomVideoView.setOnErrorListener(new OnErrorListener() {
			// @Override
			// public boolean onError(MediaPlayer mp, int what, int extra) {
			// onHideCustomView();
			// return true;
			// }
			// });
			//
			// mCustomVideoView.start();
			// }
			//
			// }
			// }
			//
			// @Override
			// public void onHideCustomView() {
			// super.onHideCustomView();
			//
			// if (mCustomVideoView == null) {
			// return;
			// }
			//
			// mCustomVideoView.setVisibility(View.GONE);
			// mCustomViewContainer.removeView(mCustomVideoView);
			// mCustomVideoView = null;
			//
			// mCustomViewContainer.setVisibility(View.GONE);
			// mCustomViewCallback.onCustomViewHidden();
			//
			// mContentView.setVisibility(View.VISIBLE);
			// setContentView(mContentView);
			// }

			@Override
			public void onProgressChanged(WebView view, int newProgress) {
				((CustomWebView) view).setProgress(newProgress);
				mProgressBar.setProgress(mCurrentWebView.getProgress());
			}

			@Override
			public void onReceivedIcon(WebView view, Bitmap icon) {
				new Thread(new FaviconUpdaterRunnable(MainActivity.this, view
						.getUrl(), view.getOriginalUrl(), icon)).start();
				updateFavIcon();

				super.onReceivedIcon(view, icon);
			}

			@Override
			public boolean onCreateWindow(WebView view, final boolean dialog,
					final boolean userGesture, final Message resultMsg) {

				WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;

				addTab(false, mViewFlipper.getDisplayedChild());

				transport.setWebView(mCurrentWebView);
				resultMsg.sendToTarget();

				return true;
			}

			@Override
			public void onReceivedTitle(WebView view, String title) {
				setTitle(String.format(
						getResources().getString(R.string.ApplicationNameUrl),
						title));

				startHistoryUpdaterRunnable(title, mCurrentWebView.getUrl(),
						mCurrentWebView.getOriginalUrl());

				super.onReceivedTitle(view, title);
			}

			@Override
			public boolean onJsAlert(WebView view, String url, String message,
					final JsResult result) {
				new AlertDialog.Builder(activity)
						.setTitle(R.string.Commons_JavaScriptDialog)
						.setMessage(message)
						.setPositiveButton(android.R.string.ok,
								new AlertDialog.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int which) {
										result.confirm();
									}
								}).setCancelable(false).create().show();

				return true;
			}

			@Override
			public boolean onJsConfirm(WebView view, String url,
					String message, final JsResult result) {
				new AlertDialog.Builder(MainActivity.this)
						.setTitle(R.string.Commons_JavaScriptDialog)
						.setMessage(message)
						.setPositiveButton(android.R.string.ok,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int which) {
										result.confirm();
									}
								})
						.setNegativeButton(android.R.string.cancel,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int which) {
										result.cancel();
									}
								}).create().show();

				return true;
			}

			@Override
			public boolean onJsPrompt(WebView view, String url, String message,
					String defaultValue, final JsPromptResult result) {

				final LayoutInflater factory = LayoutInflater
						.from(MainActivity.this);
				final View v = factory.inflate(
						R.layout.javascript_prompt_dialog, null);
				((TextView) v.findViewById(R.id.JavaScriptPromptMessage))
						.setText(message);
				((EditText) v.findViewById(R.id.JavaScriptPromptInput))
						.setText(defaultValue);

				new AlertDialog.Builder(MainActivity.this)
						.setTitle(R.string.Commons_JavaScriptDialog)
						.setView(v)
						.setPositiveButton(android.R.string.ok,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {
										String value = ((EditText) v
												.findViewById(R.id.JavaScriptPromptInput))
												.getText().toString();
										result.confirm(value);
									}
								})
						.setNegativeButton(android.R.string.cancel,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {
										result.cancel();
									}
								})
						.setOnCancelListener(
								new DialogInterface.OnCancelListener() {
									public void onCancel(DialogInterface dialog) {
										result.cancel();
									}
								}).show();

				return true;

			}

		});
	}

	private String getTouchDirection(float eventX, float eventY) {

		String direction = "";
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		float centreX = 800; // / metrics.widthPixels * eventX;
		float centreY = 480; // / metrics.heightPixels * eventY;

		// float centreX = 400/2, centreY = 700/2;
		float tx = (float) (eventX - centreX), ty = (float) (eventY - centreY);
		float radius = (float) Math.sqrt(tx * tx + ty * ty);

		float offsetX = 0, offsetY = radius, adjEventX = eventX - centreX, adjEventY = centreY
				- eventY;

		double cosaU = ((offsetX * adjEventX) + (offsetY * adjEventY));
		double cosaD = (Math.sqrt((offsetX * offsetX) + (offsetY * offsetY)) * Math
				.sqrt((adjEventX * adjEventX) + (adjEventY * adjEventY)));
		double cosa = cosaU / cosaD;

		double degr = (Math.acos(cosa) * (180 / Math.PI));

		if (adjEventX < 0)
			degr = 360 - degr;

		float offsetDegrees = (float) (degrees + degr);

		if (offsetDegrees > 360)
			offsetDegrees = offsetDegrees - 360;

		if (offsetDegrees < 22.5 || offsetDegrees > 336.5)
			direction = "NORTH";
		else if (offsetDegrees > 22.5 && offsetDegrees < 67.5)
			direction = "NORTHEAST";
		else if (offsetDegrees > 67.5 && offsetDegrees < 112.5)
			direction = "EAST";
		else if (offsetDegrees > 112.5 && offsetDegrees < 156.5)
			direction = "SOUTHEAST";
		else if (offsetDegrees > 156.5 && offsetDegrees < 201.5)
			direction = "SOUTH";
		else if (offsetDegrees > 201.5 && offsetDegrees < 246.5)
			direction = "SOUTHWEST";
		else if (offsetDegrees > 246.5 && offsetDegrees < 291.5)
			direction = "WEST";
		else if (offsetDegrees > 291.5 && offsetDegrees < 336.5)
			direction = "NORTHWEST";

		return direction;
	}

	public int getScreenOrientation() {
		Display getOrient = getActivity().getWindowManager()
				.getDefaultDisplay();
		int orientation = Configuration.ORIENTATION_UNDEFINED;
		if (getOrient.getWidth() == getOrient.getHeight()) {
			orientation = Configuration.ORIENTATION_SQUARE;
		} else {
			if (getOrient.getWidth() < getOrient.getHeight()) {
				orientation = Configuration.ORIENTATION_PORTRAIT;
			} else {
				orientation = Configuration.ORIENTATION_LANDSCAPE;
			}
		}
		return orientation;
	}

	private Activity getActivity() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Select Text in the webview and automatically sends the selected text to
	 * the clipboard.
	 */
	public void swithToSelectAndCopyTextMode() {
		try {
			KeyEvent shiftPressEvent = new KeyEvent(0, 0, KeyEvent.ACTION_DOWN,
					KeyEvent.KEYCODE_SHIFT_LEFT, 0, 0);
			shiftPressEvent.dispatch(mCurrentWebView);
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	/**
	 * Initiate a download. Check the SD card and start the download runnable.
	 * 
	 * @param url
	 *            The url to download.
	 * @param userAgent
	 *            The user agent.
	 * @param contentDisposition
	 *            The content disposition.
	 * @param mimetype
	 *            The mime type.
	 * @param contentLength
	 *            The content length.
	 */
	private void doDownloadStart(String url, String userAgent,
			String contentDisposition, String mimetype, long contentLength) {

		if (ApplicationUtils.checkCardState(this, true)) {
			DownloadItem item = new DownloadItem(this, url);
			Controller.getInstance().addToDownload(item);
			item.startDownload();

			Toast.makeText(this, getString(R.string.Main_DownloadStartedMsg),
					Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Add a new tab.
	 * 
	 * @param navigateToHome
	 *            If True, will load the user home page.
	 */
	private void addTab(boolean navigateToHome) {
		addTab(navigateToHome, -1);
	}

	/**
	 * Add a new tab.
	 * 
	 * @param navigateToHome
	 *            If True, will load the user home page.
	 * @param parentIndex
	 *            The index of the new tab.
	 */
	private void addTab(boolean navigateToHome, int parentIndex) {
		if (mFindDialogVisible) {
			closeFindDialog();
		}

		RelativeLayout view = (RelativeLayout) mInflater.inflate(
				R.layout.webview, mViewFlipper, false);

		mCurrentWebView = (CustomWebView) view.findViewById(R.id.webview);

		initializeCurrentWebView();

		synchronized (mViewFlipper) {
			if (parentIndex != -1) {
				mWebViews.add(parentIndex + 1, mCurrentWebView);
				mViewFlipper.addView(view, parentIndex + 1);
			} else {
				mWebViews.add(mCurrentWebView);
				mViewFlipper.addView(view);
			}
			mViewFlipper.setDisplayedChild(mViewFlipper.indexOfChild(view));
		}

		updateUI();
		updatePreviousNextTabViewsVisibility();

		mUrlEditText.clearFocus();

		if (navigateToHome) {
			navigateToHome();
		}
	}

	/**
	 * Remove the current tab.
	 */
	private void removeCurrentTab() {

		if (mFindDialogVisible) {
			closeFindDialog();
		}

		int removeIndex = mViewFlipper.getDisplayedChild();

		mCurrentWebView.doOnPause();

		synchronized (mViewFlipper) {
			mViewFlipper.removeViewAt(removeIndex);
			mViewFlipper.setDisplayedChild(removeIndex - 1);
			mWebViews.remove(removeIndex);
		}

		mCurrentWebView = mWebViews.get(mViewFlipper.getDisplayedChild());

		updateUI();
		updatePreviousNextTabViewsVisibility();

		mUrlEditText.clearFocus();
	}

	private void doFind() {
		CharSequence find = mFindText.getText();
		if (find.length() == 0) {
			mFindPreviousButton.setEnabled(false);
			mFindNextButton.setEnabled(false);
			mCurrentWebView.clearMatches();
		} else {
			int found = mCurrentWebView.findAll(find.toString());
			if (found < 2) {
				mFindPreviousButton.setEnabled(false);
				mFindNextButton.setEnabled(false);
			} else {
				mFindPreviousButton.setEnabled(true);
				mFindNextButton.setEnabled(true);
			}
		}
	}

	private void showFindDialog() {
		setFindBarVisibility(true);
		mCurrentWebView.doSetFindIsUp(true);
		CharSequence text = mFindText.getText();
		if (text.length() > 0) {
			mFindText.setSelection(0, text.length());
			doFind();
		} else {
			mFindPreviousButton.setEnabled(false);
			mFindNextButton.setEnabled(false);
		}

		mFindText.requestFocus();
		showKeyboardForFindDialog();
	}

	private void closeFindDialog() {
		hideKeyboardFromFindDialog();
		mCurrentWebView.doNotifyFindDialogDismissed();
		setFindBarVisibility(false);
	}

	private void setFindBarVisibility(boolean visible) {
		if (visible) {
			mFindBar.startAnimation(AnimationManager.getInstance()
					.getTopBarShowAnimation());
			mFindBar.setVisibility(View.VISIBLE);
			mFindDialogVisible = true;
		} else {
			mFindBar.startAnimation(AnimationManager.getInstance()
					.getTopBarHideAnimation());
			mFindBar.setVisibility(View.GONE);
			mFindDialogVisible = false;
		}
	}

	/**
	 * Change the tool bars visibility.
	 * 
	 * @param visible
	 *            If True, the tool bars will be shown.
	 */
	private void setToolbarsVisibility(boolean visible) {

		boolean switchTabByButtons = isSwitchTabsByButtonsEnabled();
		boolean showPreviousTabView = mViewFlipper.getDisplayedChild() > 0;
		boolean showNextTabView = mViewFlipper.getDisplayedChild() < mViewFlipper
				.getChildCount() - 1;

		if (visible) {

			if (!mUrlBarVisible) {
				mTopBar.startAnimation(AnimationManager.getInstance()
						.getTopBarShowAnimation());
				mBottomBar.startAnimation(AnimationManager.getInstance()
						.getBottomBarShowAnimation());

				if (switchTabByButtons) {
					if (showPreviousTabView) {
						mPreviousTabView.startAnimation(AnimationManager
								.getInstance()
								.getPreviousTabViewShowAnimation());
					}

					if (showNextTabView) {
						mNextTabView.startAnimation(AnimationManager
								.getInstance().getNextTabViewShowAnimation());
					}
				}

				mTopBar.setVisibility(View.VISIBLE);
				mBottomBar.setVisibility(View.VISIBLE);

				if (switchTabByButtons) {
					if (showPreviousTabView) {
						mPreviousTabView.setVisibility(View.VISIBLE);
					}

					if (showNextTabView) {
						mNextTabView.setVisibility(View.VISIBLE);
					}
				}

				mBubbleRightView.setVisibility(View.GONE);
				mBubbleLeftView.setVisibility(View.GONE);
			}

			startToolbarsHideRunnable();

			mUrlBarVisible = true;

		} else {

			if (mUrlBarVisible) {
				mTopBar.startAnimation(AnimationManager.getInstance()
						.getTopBarHideAnimation());
				mBottomBar.startAnimation(AnimationManager.getInstance()
						.getBottomBarHideAnimation());

				if (switchTabByButtons) {
					if (showPreviousTabView) {
						mPreviousTabView.startAnimation(AnimationManager
								.getInstance()
								.getPreviousTabViewHideAnimation());
					}

					if (showNextTabView) {
						mNextTabView.startAnimation(AnimationManager
								.getInstance().getNextTabViewHideAnimation());
					}
				}

				mTopBar.setVisibility(View.GONE);
				mBottomBar.setVisibility(View.GONE);

				if (switchTabByButtons) {
					if (showPreviousTabView) {
						mPreviousTabView.setVisibility(View.GONE);
					}

					if (showNextTabView) {
						mNextTabView.setVisibility(View.GONE);
					}
				}

				String bubblePosition = Controller
						.getInstance()
						.getPreferences()
						.getString(
								Constants.PREFERENCES_GENERAL_BUBBLE_POSITION,
								"right");

				if (bubblePosition.equals("right")) {
					mBubbleRightView.setVisibility(View.VISIBLE);
					mBubbleLeftView.setVisibility(View.GONE);
				} else if (bubblePosition.equals("left")) {
					mBubbleRightView.setVisibility(View.GONE);
					mBubbleLeftView.setVisibility(View.VISIBLE);
				} else if (bubblePosition.equals("both")) {
					mBubbleRightView.setVisibility(View.VISIBLE);
					mBubbleLeftView.setVisibility(View.VISIBLE);
				} else {
					mBubbleRightView.setVisibility(View.VISIBLE);
					mBubbleLeftView.setVisibility(View.GONE);
				}
			}

			mUrlBarVisible = false;
		}
	}

	private void showKeyboardForFindDialog() {
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.showSoftInput(mFindText, InputMethodManager.SHOW_IMPLICIT);
	}

	private void hideKeyboardFromFindDialog() {
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(mFindText.getWindowToken(), 0);
	}

	/**
	 * Hide the keyboard.
	 * 
	 * @param delayedHideToolbars
	 *            If True, will start a runnable to delay tool bars hiding. If
	 *            False, tool bars are hidden immediatly.
	 */
	private void hideKeyboard(boolean delayedHideToolbars) {
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(mUrlEditText.getWindowToken(), 0);

		if (mUrlBarVisible) {
			if (delayedHideToolbars) {
				startToolbarsHideRunnable();
			} else {
				setToolbarsVisibility(false);
			}
		}
	}

	/**
	 * Thread to delay the show of the find dialog. This seems to be necessary
	 * when shown from a QuickAction. If not, the keyboard does not show. 50ms
	 * seems to be enough on a Nexus One and on the (rather) slow emulator.
	 * Dirty hack :(
	 */
	private void startShowFindDialogRunnable() {
		new Thread(new Runnable() {

			private Handler mHandler = new Handler() {
				public void handleMessage(Message msg) {
					showFindDialog();
				}
			};

			@Override
			public void run() {
				try {
					Thread.sleep(50);
					mHandler.sendEmptyMessage(0);
				} catch (InterruptedException e) {
					mHandler.sendEmptyMessage(0);
				}

			}
		}).start();
	}

	/**
	 * Start a runnable to hide the tool bars after a user-defined delay.
	 */
	private void startToolbarsHideRunnable() {

		if (mHideToolbarsRunnable != null) {
			mHideToolbarsRunnable.setDisabled();
		}

		int delay = Integer
				.parseInt(Controller
						.getInstance()
						.getPreferences()
						.getString(Constants.PREFERENCES_GENERAL_BARS_DURATION,
								"3000"));
		if (delay <= 0) {
			delay = 3000;
		}

		mHideToolbarsRunnable = new HideToolbarsRunnable(this, delay);
		new Thread(mHideToolbarsRunnable).start();
	}

	/**
	 * Hide the tool bars.
	 */
	public void hideToolbars() {
		if (mUrlBarVisible) {
			if ((!mUrlEditText.hasFocus()) && (!mToolsActionGridVisible)) {

				if (!mCurrentWebView.isLoading()) {
					setToolbarsVisibility(false);
				}
			}
		}
		mHideToolbarsRunnable = null;
	}

	/**
	 * Start a runnable to update history.
	 * 
	 * @param title
	 *            The page title.
	 * @param url
	 *            The page url.
	 */
	private void startHistoryUpdaterRunnable(String title, String url,
			String originalUrl) {
		if ((url != null) && (url.length() > 0)) {
			new Thread(new HistoryUpdater(this, title, url, originalUrl))
					.start();
		}
	}

	/**
	 * Navigate to the given url.
	 * 
	 * @param url
	 *            The url.
	 */
	private void navigateToUrl(String url) {
		// Needed to hide toolbars properly.
		mUrlEditText.clearFocus();

		if ((url != null) && (url.length() > 0)) {

			if (UrlUtils.isUrl(url)) {
				url = UrlUtils.checkUrl(url);
			} else {
				url = UrlUtils.getSearchUrl(this, url);
			}

			hideKeyboard(true);

			if (url.equals(Constants.URL_ABOUT_START)) {

				mCurrentWebView.loadDataWithBaseURL(
						"file:///android_asset/startpage/",
						ApplicationUtils.getStartPage(this), "text/html",
						"UTF-8", Constants.URL_ABOUT_START);

			} else {

				// If the url is not from GWT mobile view, and is in the mobile
				// view url list, then load it with GWT.
				if ((!url
						.startsWith(Constants.URL_GOOGLE_MOBILE_VIEW_NO_FORMAT))
						&& (UrlUtils.checkInMobileViewUrlList(this, url))) {

					url = String.format(Constants.URL_GOOGLE_MOBILE_VIEW, url);
				}

				mCurrentWebView.loadUrl(url);
			}
		}
	}

	/**
	 * Navigate to the url given in the url edit text.
	 */
	private void navigateToUrl() {
		navigateToUrl(mUrlEditText.getText().toString());
	}

	/**
	 * Navigate to the user home page.
	 */
	private void navigateToHome() {
		navigateToUrl(Controller
				.getInstance()
				.getPreferences()
				.getString(Constants.PREFERENCES_GENERAL_HOME_PAGE,
						Constants.URL_ABOUT_START));
	}

	/**
	 * Navigate to the previous page in history.
	 */
	private void navigatePrevious() {
		// Needed to hide toolbars properly.
		mUrlEditText.clearFocus();

		hideKeyboard(true);
		mCurrentWebView.goBack();
	}

	/**
	 * Navigate to the next page in history.
	 */
	private void navigateNext() {
		// Needed to hide toolbars properly.
		mUrlEditText.clearFocus();

		hideKeyboard(true);
		mCurrentWebView.goForward();
	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {

		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			this.moveTaskToBack(true);
			return true;
		default:
			return super.onKeyLongPress(keyCode, event);
		}
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {

		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			if (mCustomView != null) {
				hideCustomView();
			} else if (mFindDialogVisible) {
				closeFindDialog();
			} else {
				if (mCurrentWebView.canGoBack()) {
					mCurrentWebView.goBack();
				} else {
					this.moveTaskToBack(true);
				}
			}
			return true;
		case KeyEvent.KEYCODE_SEARCH:
			if (!mFindDialogVisible) {
				showFindDialog();
			}
			return true;
		case KeyEvent.KEYCODE_VOLUME_DOWN:
		case KeyEvent.KEYCODE_VOLUME_UP:
			String volumeKeysBehaviour = PreferenceManager
					.getDefaultSharedPreferences(this).getString(
							Constants.PREFERENCES_UI_VOLUME_KEYS_BEHAVIOUR,
							"DEFAULT");

			if (volumeKeysBehaviour.equals("DEFAULT")) {
				return super.onKeyUp(keyCode, event);
			} else {
				return true;
			}
		default:
			return super.onKeyUp(keyCode, event);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		String volumeKeysBehaviour = PreferenceManager
				.getDefaultSharedPreferences(this).getString(
						Constants.PREFERENCES_UI_VOLUME_KEYS_BEHAVIOUR,
						"DEFAULT");

		if (!volumeKeysBehaviour.equals("DEFAULT")) {
			switch (keyCode) {
			case KeyEvent.KEYCODE_VOLUME_DOWN:

				if (volumeKeysBehaviour.equals("SWITCH_TABS")) {
					showPreviousTab(false);
				} else if (volumeKeysBehaviour.equals("SCROLL")) {
					mCurrentWebView.pageDown(false);
				} else if (volumeKeysBehaviour.equals("HISTORY")) {
					mCurrentWebView.goForward();
				} else {
					mCurrentWebView.zoomIn();
				}

				return true;
			case KeyEvent.KEYCODE_VOLUME_UP:

				if (volumeKeysBehaviour.equals("SWITCH_TABS")) {
					showNextTab(false);
				} else if (volumeKeysBehaviour.equals("SCROLL")) {
					mCurrentWebView.pageUp(false);
				} else if (volumeKeysBehaviour.equals("HISTORY")) {
					mCurrentWebView.goBack();
				} else {
					mCurrentWebView.zoomOut();
				}

				return true;
			default:
				return super.onKeyDown(keyCode, event);
			}
		} else {
			return super.onKeyDown(keyCode, event);
		}
	}

	/**
	 * Set the application title to default.
	 */
	private void clearTitle() {
		this.setTitle(getResources().getString(R.string.ApplicationName));
	}

	/**
	 * Update the application title.
	 */
	private void updateTitle() {
		String value = mCurrentWebView.getTitle();

		if ((value != null) && (value.length() > 0)) {
			this.setTitle(String.format(
					getResources().getString(R.string.ApplicationNameUrl),
					value));
		} else {
			clearTitle();
		}
	}

	/**
	 * Get a Drawable of the current favicon, with its size normalized relative
	 * to current screen density.
	 * 
	 * @return The normalized favicon.
	 */
	private BitmapDrawable getNormalizedFavicon() {

		BitmapDrawable favIcon = new BitmapDrawable(getResources(),
				mCurrentWebView.getFavicon());

		if (mCurrentWebView.getFavicon() != null) {
			int imageButtonSize = ApplicationUtils.getImageButtonSize(this);
			int favIconSize = ApplicationUtils.getFaviconSize(this);

			Bitmap bm = Bitmap.createBitmap(imageButtonSize, imageButtonSize,
					Bitmap.Config.ARGB_4444);
			Canvas canvas = new Canvas(bm);

			favIcon.setBounds((imageButtonSize / 2) - (favIconSize / 2),
					(imageButtonSize / 2) - (favIconSize / 2),
					(imageButtonSize / 2) + (favIconSize / 2),
					(imageButtonSize / 2) + (favIconSize / 2));
			favIcon.draw(canvas);

			favIcon = new BitmapDrawable(getResources(), bm);
		}

		return favIcon;
	}

	/**
	 * Update the "Go" button image.
	 */
	private void updateGoButton() {
		if (mCurrentWebView.isLoading()) {
			mGoButton.setImageResource(R.drawable.ic_btn_stop);
			mUrlEditText.setCompoundDrawablesWithIntrinsicBounds(null, null,
					mCircularProgress, null);
			((AnimationDrawable) mCircularProgress).start();
		} else {
			if (!mCurrentWebView.isSameUrl(mUrlEditText.getText().toString())) {
				mGoButton.setImageResource(R.drawable.ic_btn_go);
			} else {
				mGoButton.setImageResource(R.drawable.ic_btn_reload);
			}

			mUrlEditText.setCompoundDrawablesWithIntrinsicBounds(null, null,
					null, null);
			((AnimationDrawable) mCircularProgress).stop();
		}
	}

	/**
	 * Update the fav icon display.
	 */
	private void updateFavIcon() {
		BitmapDrawable favicon = getNormalizedFavicon();

		if (mCurrentWebView.getFavicon() != null) {
			mToolsButton.setImageDrawable(favicon);
		} else {
			mToolsButton.setImageResource(R.drawable.fav_icn_default_toolbar);
		}
	}

	/**
	 * Update the UI: Url edit text, previous/next button state,...
	 */
	private void updateUI() {
		mUrlEditText.removeTextChangedListener(mUrlTextWatcher);
		mUrlEditText.setText(mCurrentWebView.getUrl());
		mUrlEditText.addTextChangedListener(mUrlTextWatcher);

		mPreviousButton.setEnabled(mCurrentWebView.canGoBack());
		mNextButton.setEnabled(mCurrentWebView.canGoForward());

		if (mCurrentWebView.getUrl() != null)
			mRemoveTabButton
					.setEnabled((mViewFlipper.getChildCount() > 1 || !mCurrentWebView
							.getUrl().equals(Constants.URL_ABOUT_START)));
		else
			mRemoveTabButton.setEnabled(mViewFlipper.getChildCount() > 1);

		mProgressBar.setProgress(mCurrentWebView.getProgress());

		updateGoButton();

		updateTitle();

		updateFavIcon();
	}

	private boolean isSwitchTabsByFlingEnabled() {
		return (mSwitchTabsMethod == SwitchTabsMethod.FLING)
				|| (mSwitchTabsMethod == SwitchTabsMethod.BOTH);
	}

	private boolean isSwitchTabsByButtonsEnabled() {
		return (mSwitchTabsMethod == SwitchTabsMethod.BUTTONS)
				|| (mSwitchTabsMethod == SwitchTabsMethod.BOTH);
	}

	/**
	 * Open the "Add bookmark" dialog.
	 */
	private void openAddBookmarkDialog() {
		Intent i = new Intent(this, EditBookmarkActivity.class);

		i.putExtra(Constants.EXTRA_ID_BOOKMARK_ID, (long) -1);
		i.putExtra(Constants.EXTRA_ID_BOOKMARK_TITLE,
				mCurrentWebView.getTitle());
		i.putExtra(Constants.EXTRA_ID_BOOKMARK_URL, mCurrentWebView.getUrl());

		startActivity(i);
	}

	/**
	 * Open the bookmark list.
	 */
	private void openBookmarksHistoryActivity() {
		Intent i = new Intent(this, BookmarksHistoryActivity.class);
		startActivityForResult(i, OPEN_BOOKMARKS_HISTORY_ACTIVITY);
	}

	/**
	 * Open the download list.
	 */
	private void openDownloadsList() {
		Intent i = new Intent(this, DownloadsListActivity.class);
		startActivityForResult(i, OPEN_DOWNLOADS_ACTIVITY);
	}

	/**
	 * Perform the user-defined action when clicking on the quick button.
	 */
	private void onQuickButton() {
		openBookmarksHistoryActivity();
	}

	/**
	 * Open preferences.
	 */
	private void openPreferences() {
		Intent preferencesActivity = new Intent(this, PreferencesActivity.class);
		startActivity(preferencesActivity);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuItem item;

		item = menu.add(0, MENU_ADD_BOOKMARK, 0, R.string.Main_MenuAddBookmark);
		item.setIcon(R.drawable.ic_menu_add_bookmark);

		item = menu.add(0, MENU_SHOW_BOOKMARKS, 0,
				R.string.Main_MenuShowBookmarks);
		item.setIcon(R.drawable.ic_menu_bookmarks);

		item = menu.add(0, MENU_SHOW_DOWNLOADS, 0,
				R.string.Main_MenuShowDownloads);
		item.setIcon(R.drawable.ic_menu_downloads);

		item = menu.add(0, MENU_PREFERENCES, 0, R.string.Main_MenuPreferences);
		item.setIcon(R.drawable.ic_menu_preferences);

		item = menu.add(0, MENU_EXIT, 0, R.string.Main_MenuExit);
		item.setIcon(R.drawable.ic_menu_exit);

		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ADD_BOOKMARK:
			openAddBookmarkDialog();
			return true;
		case MENU_SHOW_BOOKMARKS:
			openBookmarksHistoryActivity();
			return true;
		case MENU_SHOW_DOWNLOADS:
			openDownloadsList();
			return true;
		case MENU_PREFERENCES:
			openPreferences();
			return true;
		case MENU_EXIT:
			this.finish();
			return true;
		default:
			return super.onMenuItemSelected(featureId, item);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

		if (requestCode == OPEN_BOOKMARKS_HISTORY_ACTIVITY) {
			if (intent != null) {
				Bundle b = intent.getExtras();
				if (b != null) {
					if (b.getBoolean(Constants.EXTRA_ID_NEW_TAB)) {
						addTab(false);
					}
					navigateToUrl(b.getString(Constants.EXTRA_ID_URL));
				}
			}
		} else if (requestCode == OPEN_FILE_CHOOSER_ACTIVITY) {
			if (mUploadMessage == null) {
				return;
			}

			Uri result = intent == null || resultCode != RESULT_OK ? null
					: intent.getData();
			mUploadMessage.onReceiveValue(result);
			mUploadMessage = null;
		}
	}

	@Override
	protected void onPause() {
		mCurrentWebView.doOnPause();
		super.onPause();
		// mSensorManager.unregisterListener(this);
	}

	@Override
	protected void onResume() {
		mCurrentWebView.doOnResume();
		super.onResume();
		// mSensorManager.registerListener(this, mLight,
		// SensorManager.SENSOR_DELAY_NORMAL);
		Sensor gsensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
		Sensor asensor = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		Sensor msensor = mSensorManager
				.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		mSensorManager.registerListener(this, gsensor,
				SensorManager.SENSOR_DELAY_GAME);
		mSensorManager.registerListener(this, asensor,
				SensorManager.SENSOR_DELAY_GAME);
		mSensorManager.registerListener(this, msensor,
				SensorManager.SENSOR_DELAY_GAME);

	}

	/**
	 * Show a toast alert on tab switch.
	 */
	private void showToastOnTabSwitch() {
		if (Controller
				.getInstance()
				.getPreferences()
				.getBoolean(Constants.PREFERENCES_SHOW_TOAST_ON_TAB_SWITCH,
						true)) {
			String text;
			if (mCurrentWebView.getTitle() != null) {
				text = String.format(
						getString(R.string.Main_ToastTabSwitchFullMessage),
						mViewFlipper.getDisplayedChild() + 1,
						mCurrentWebView.getTitle());
			} else {
				text = String.format(
						getString(R.string.Main_ToastTabSwitchMessage),
						mViewFlipper.getDisplayedChild() + 1);
			}
			Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
			Log.d("Venki", String.valueOf(mViewFlipper.getDisplayedChild() + 1));
			int tab = mViewFlipper.getDisplayedChild() + 1;
		}
	}

	private void updatePreviousNextTabViewsVisibility() {
		if ((mUrlBarVisible) && (isSwitchTabsByButtonsEnabled())) {
			if (mViewFlipper.getDisplayedChild() > 0) {
				mPreviousTabView.setVisibility(View.VISIBLE);
			} else {
				mPreviousTabView.setVisibility(View.GONE);
			}

			if (mViewFlipper.getDisplayedChild() < mViewFlipper.getChildCount() - 1) {
				mNextTabView.setVisibility(View.VISIBLE);
			} else {
				mNextTabView.setVisibility(View.GONE);
			}
		} else {
			mPreviousTabView.setVisibility(View.GONE);
			mNextTabView.setVisibility(View.GONE);
		}
	}

	/**
	 * Show the previous tab, if any.
	 */
	private void showPreviousTab(boolean resetToolbarsRunnable) {

		if (mViewFlipper.getChildCount() > 1) {

			if (mFindDialogVisible) {
				closeFindDialog();
			}

			mCurrentWebView.doOnPause();

			mViewFlipper.setInAnimation(AnimationManager.getInstance()
					.getInFromLeftAnimation());
			mViewFlipper.setOutAnimation(AnimationManager.getInstance()
					.getOutToRightAnimation());

			mViewFlipper.showPrevious();

			mCurrentWebView = mWebViews.get(mViewFlipper.getDisplayedChild());

			mCurrentWebView.doOnResume();

			if (resetToolbarsRunnable) {
				startToolbarsHideRunnable();
			}

			showToastOnTabSwitch();

			updatePreviousNextTabViewsVisibility();

			updateUI();
		}
	}

	private void showCustomView(View view,
			WebChromeClient.CustomViewCallback callback) {
		// if a view already exists then immediately terminate the new one
		if (mCustomView != null) {
			callback.onCustomViewHidden();
			return;
		}

		MainActivity.this.getWindow().getDecorView();

		FrameLayout decor = (FrameLayout) getWindow().getDecorView();
		mFullscreenContainer = new FullscreenHolder(MainActivity.this);
		mFullscreenContainer.addView(view, COVER_SCREEN_PARAMS);
		decor.addView(mFullscreenContainer, COVER_SCREEN_PARAMS);
		mCustomView = view;
		setStatusBarVisibility(false);
		mCustomViewCallback = callback;
	}

	private void hideCustomView() {
		if (mCustomView == null)
			return;

		setStatusBarVisibility(true);
		FrameLayout decor = (FrameLayout) getWindow().getDecorView();
		decor.removeView(mFullscreenContainer);
		mFullscreenContainer = null;
		mCustomView = null;
		mCustomViewCallback.onCustomViewHidden();
	}

	/**
	 * Show the next tab, if any.
	 */
	private void showNextTab(boolean resetToolbarsRunnable) {

		if (mViewFlipper.getChildCount() > 1) {

			if (mFindDialogVisible) {
				closeFindDialog();
			}

			mCurrentWebView.doOnPause();

			mViewFlipper.setInAnimation(AnimationManager.getInstance()
					.getInFromRightAnimation());
			mViewFlipper.setOutAnimation(AnimationManager.getInstance()
					.getOutToLeftAnimation());

			mViewFlipper.showNext();

			mCurrentWebView = mWebViews.get(mViewFlipper.getDisplayedChild());

			mCurrentWebView.doOnResume();

			if (resetToolbarsRunnable) {
				startToolbarsHideRunnable();
			}

			showToastOnTabSwitch();

			updatePreviousNextTabViewsVisibility();

			updateUI();
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {

		hideKeyboard(false);

		return mGestureDetector.onTouchEvent(event);
	}

	/**
	 * Check if the url is in the AdBlock white list.
	 * 
	 * @param url
	 *            The url to check
	 * @return true if the url is in the white list
	 */
	private boolean checkInAdBlockWhiteList(String url) {

		if (url != null) {
			boolean inList = false;
			Iterator<String> iter = Controller.getInstance()
					.getAdBlockWhiteList(this).iterator();
			while ((iter.hasNext()) && (!inList)) {
				if (url.contains(iter.next())) {
					inList = true;
				}
			}
			return inList;
		} else {
			return false;
		}
	}

	public void onPageFinished(String url) {
		updateUI();

		if ((Controller.getInstance().getPreferences().getBoolean(
				Constants.PREFERENCES_ADBLOCKER_ENABLE, true))
				&& (!checkInAdBlockWhiteList(mCurrentWebView.getUrl()))) {
			mCurrentWebView.loadAdSweep();
		}

		WebIconDatabase.getInstance().retainIconForPageUrl(
				mCurrentWebView.getUrl());

		if (mUrlBarVisible) {
			startToolbarsHideRunnable();
		}
	}

	public void onPageStarted(String url) {
		if (mFindDialogVisible) {
			closeFindDialog();
		}

		mUrlEditText.removeTextChangedListener(mUrlTextWatcher);
		mUrlEditText.setText(url);
		mUrlEditText.addTextChangedListener(mUrlTextWatcher);

		mPreviousButton.setEnabled(false);
		mNextButton.setEnabled(false);

		updateGoButton();

		setToolbarsVisibility(true);
	}

	public void onUrlLoading(String url) {
		setToolbarsVisibility(true);
	}

	public void onMailTo(String url) {
		Intent sendMail = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		startActivity(sendMail);
	}

	public void onExternalApplicationUrl(String url) {
		try {

			Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			startActivity(i);

		} catch (Exception e) {

			// Notify user that the vnd url cannot be viewed.
			new AlertDialog.Builder(this)
					.setTitle(R.string.Main_VndErrorTitle)
					.setMessage(
							String.format(
									getString(R.string.Main_VndErrorMessage),
									url))
					.setPositiveButton(android.R.string.ok,
							new AlertDialog.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
								}
							}).setCancelable(true).create().show();
		}
	}

	public void setHttpAuthUsernamePassword(String host, String realm,
			String username, String password) {
		mCurrentWebView.setHttpAuthUsernamePassword(host, realm, username,
				password);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {

		if ((item != null) && (item.getIntent() != null)) {
			Bundle b = item.getIntent().getExtras();

			switch (item.getItemId()) {
			case CONTEXT_MENU_OPEN:
				if (b != null) {
					navigateToUrl(b.getString(Constants.EXTRA_ID_URL));
				}
				return true;

			case CONTEXT_MENU_OPEN_IN_NEW_TAB:
				if (b != null) {
					addTab(false, mViewFlipper.getDisplayedChild());
					navigateToUrl(b.getString(Constants.EXTRA_ID_URL));
				}
				return true;

			case CONTEXT_MENU_DOWNLOAD:
				if (b != null) {
					doDownloadStart(b.getString(Constants.EXTRA_ID_URL), null,
							null, null, 0);
				}
				return true;
			case CONTEXT_MENU_COPY:
				if (b != null) {
					ApplicationUtils.copyTextToClipboard(this,
							b.getString(Constants.EXTRA_ID_URL),
							getString(R.string.Commons_UrlCopyToastMessage));
				}
				return true;
			case CONTEXT_MENU_SHARE:
				if (b != null) {
					ApplicationUtils.sharePage(this, "",
							b.getString(Constants.EXTRA_ID_URL));
				}
				return true;
			default:
				return super.onContextItemSelected(item);
			}
		}

		return super.onContextItemSelected(item);
	}

	@Override
	public void onDownloadEvent(String event, Object data) {
		if (event.equals(EventConstants.EVT_DOWNLOAD_ON_FINISHED)) {

			DownloadItem item = (DownloadItem) data;

			if (item.getErrorMessage() == null) {
				Toast.makeText(this,
						getString(R.string.Main_DownloadFinishedMsg),
						Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(
						this,
						getString(R.string.Main_DownloadErrorMsg,
								item.getErrorMessage()), Toast.LENGTH_SHORT)
						.show();
			}
		}
	}

	/**
	 * Gesture listener implementation.
	 */
	private class GestureListener extends
			GestureDetector.SimpleOnGestureListener {

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			mCurrentWebView.zoomIn();
			return super.onDoubleTap(e);
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			if (isSwitchTabsByFlingEnabled()) {
				if (e2.getEventTime() - e1.getEventTime() <= FLIP_TIME_THRESHOLD) {
					if (e2.getX() > (e1.getX() + FLIP_PIXEL_THRESHOLD)) {

						showPreviousTab(false);
						return false;
					}

					// going forwards: pushing stuff to the left
					if (e2.getX() < (e1.getX() - FLIP_PIXEL_THRESHOLD)) {

						showNextTab(false);
						return false;
					}
				}
			}

			return super.onFling(e1, e2, velocityX, velocityY);
		}

	}

	static class FullscreenHolder extends FrameLayout {

		public FullscreenHolder(Context ctx) {
			super(ctx);
			setBackgroundColor(ctx.getResources().getColor(
					android.R.color.black));
		}

		// venki
		@Override
		public boolean onTouchEvent(MotionEvent evt) {
			return detector.onTouchEvent(evt);
		}

	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		/*
		 * float lux = arg0.values[SensorManager.DATA_X]; //float lux1 =
		 * arg0.values[1]; float lux1=arg0.values[SensorManager.DATA_Y]; float
		 * lux2 = arg0.values[SensorManager.DATA_Z]; String strA=
		 * String.valueOf(
		 * lux)+'\t'+String.valueOf(lux1)+'\t'+String.valueOf(lux2
		 * )+'\t'+System.currentTimeMillis()+'\t'+'\n'; try {
		 * out_stream1.write(strA.getBytes()); } catch (IOException e) { // TODO
		 * Auto-generated catch block e.printStackTrace(); }
		 */

		float[] data;
		int tab = mViewFlipper.getDisplayedChild() + 1;
		Calendar c = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
		String currentDateandTime = sdf.format(new Date());

		switch (event.sensor.getType()) {
		/*
		 * case Sensor.TYPE_GRAVITY: { gData[0] = event.values[0]; gData[1] =
		 * event.values[1]; gData[2] = event.values[2]; haveGrav = true; String
		 * strG=
		 * "Gyro"+'\t'+String.valueOf(gData[0])+'\t'+String.valueOf(gData[1
		 * ])+'\t'
		 * +String.valueOf(gData[2])+'\t'+System.currentTimeMillis()+'\t'+'\n';
		 * try { out_stream1.write(strG.getBytes()); } catch (IOException e) {
		 * // TODO Auto-generated catch block e.printStackTrace(); } } break;
		 */
		case Sensor.TYPE_ACCELEROMETER: {
			// if (haveGrav) break; // don't need it, we have better
			gData[0] = event.values[0];
			gData[1] = event.values[1];
			gData[2] = event.values[2];
			haveAccel = true;

			String strA = "Accelometer" + '\t' + String.valueOf(gData[0])
					+ '\t' + String.valueOf(gData[1]) + '\t'
					+ String.valueOf(gData[2]) + '\t' + c.getTimeInMillis()
					+ '\t' + currentDateandTime + '\t' + '\n';
			if (tab == 1)

			{
				try {
					out_stream5.write(strA.getBytes());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			if (tab == 2) {
				try {
					out_stream6.write(strA.getBytes());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			if (tab == 3) {
				try {
					out_stream7.write(strA.getBytes());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (tab == 4) {
				try {
					out_stream8.write(strA.getBytes());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			if (tab == 5) {
				try {
					out_stream9.write(strA.getBytes());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
			break;
		case Sensor.TYPE_MAGNETIC_FIELD: {
			mData[0] = event.values[0];
			mData[1] = event.values[1];
			mData[2] = event.values[2];
			haveMag = true;
			String strM = "Magnometer" + '\t' + String.valueOf(mData[0]) + '\t'
					+ String.valueOf(mData[1]) + '\t'
					+ String.valueOf(mData[2]) + '\t' + c.getTimeInMillis()
					+ '\t' + currentDateandTime + '\t' + '\n';
			if (tab == 1)

			{
				try {
					out_stream5.write(strM.getBytes());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			if (tab == 2) {
				try {
					out_stream6.write(strM.getBytes());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			if (tab == 3) {
				try {
					out_stream7.write(strM.getBytes());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (tab == 4) {
				try {
					out_stream8.write(strM.getBytes());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			if (tab == 5) {
				try {
					out_stream9.write(strM.getBytes());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
			break;
		case Sensor.TYPE_PROXIMITY: {
			mData[0] = event.values[0];
			mData[1] = event.values[1];
			mData[2] = event.values[2];
			haveMag = true;
			String strM = "Proximity" + '\t' + String.valueOf(mData[0]) + '\t'
					+ String.valueOf(mData[1]) + '\t'
					+ String.valueOf(mData[2]) + '\t' + c.getTimeInMillis()
					+ '\t' + currentDateandTime + '\t' + '\n';
			if (tab == 1)

			{
				try {
					out_stream5.write(strM.getBytes());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			if (tab == 2) {
				try {
					out_stream6.write(strM.getBytes());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			if (tab == 3) {
				try {
					out_stream7.write(strM.getBytes());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (tab == 4) {
				try {
					out_stream8.write(strM.getBytes());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			if (tab == 5) {
				try {
					out_stream9.write(strM.getBytes());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
			break;
		/*
		 * case Sensor..TYPE_RELATIVE_HUMIDITY: { mData[0] = event.values[0];
		 * mData[1] = event.values[1]; mData[2] = event.values[2]; haveMag =
		 * true; String strM=
		 * "TYPE_RELATIVE_HUMIDITY"+'\t'+String.valueOf(gData[
		 * 0])+'\t'+String.valueOf
		 * (gData[1])+'\t'+String.valueOf(gData[2])+'\t'+'\n'; try {
		 * out_stream1.write(strM.getBytes()); } catch (IOException e) { // TODO
		 * Auto-generated catch block e.printStackTrace(); } } break;
		 */
		default:
			// break;
			return;
		}

	}

	/*
	 * //@Override protected void onResume1() { super.onResume();
	 * mSensorManager.registerListener(this, mLight,
	 * SensorManager.SENSOR_DELAY_NORMAL); }
	 * 
	 * // @Override protected void onPause1() { super.onPause();
	 * mSensorManager.unregisterListener(this); }
	 */
	protected void onStop() {
		super.onStop();

		String filepath = file1.getAbsolutePath();
		String filepath1 = file2.getAbsolutePath();
		String filepath2 = file3.getAbsolutePath();
		String filepath3 = file4.getAbsolutePath();
		String filepath4 = file5.getAbsolutePath();
		String filepath5 = file6.getAbsolutePath();
		try {
			Log.d(TAG, "Before uploading" + file1.getAbsolutePath());
			out_stream.flush();
			upload(filepath);
			out_stream1.flush();
			upload1(filepath1);
			out_stream2.flush();
			upload2(filepath2);
			out_stream3.flush();
			upload3(filepath3);
			out_stream4.flush();
			upload4(filepath4);
			out_stream5.flush();
			upload5(filepath5);
			// upload(filepath1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// try {
		// upload1(filepath1);
		// // upload(filepath1);
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// try {
		// upload2(filepath2);
		// // upload(filepath1);
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// try {
		// upload3(filepath3);
		// // upload(filepath1);
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// try {
		// upload4(filepath4);
		// // upload(filepath1);
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// try {
		// upload5(filepath5);
		// // upload(filepath1);
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		/*
		 * File tmpFile = new File(filepath,
		 * "IMG_2012-03-12_10-22-09_thumb.jpg");
		 * 
		 * FileInputStream fis = new FileInputStream(tmpFile);
		 * 
		 * try { DropboxAPI.Entry newEntry =
		 * mDBApi.putFileOverwrite("IMG_2012-03-12_10-22-09_thumb.jpg", fis,
		 * tmpFile.length(), null); } catch (DropboxUnlinkedException e) {
		 * Log.e("DbExampleLog", "User has unlinked."); } catch
		 * (DropboxException e) { Log.e("DbExampleLog",
		 * "Something went wrong while uploading."); }
		 */
	}

	String fpath;
	String fpath1;
	String fpath2;
	String fpath3;
	String fpath4;
	String fpath5;

	public void upload(String filepath) throws IOException {
		fpath = filepath;
		Runnable r = new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub

				HttpClient httpclient = new DefaultHttpClient();
				httpclient.getParams().setParameter(
						CoreProtocolPNames.PROTOCOL_VERSION,
						HttpVersion.HTTP_1_1);
				HttpPost httppost = new HttpPost(
						"https://sprdatalink2014.poly.edu/Testing/file1.php");
				// try {
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(
						2);
				// nameValuePairs.add(new BasicNameValuePair("id", "01"));
				// nameValuePairs.add(new BasicNameValuePair("bvs233",
				// "vraelrulz"));
				// httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				// httpclient.execute(httppost);
				// msgTextField.setText(""); //reset the message text field
				// Toast.makeText(getBaseContext(),"connection established",Toast.LENGTH_SHORT).show();
				// } catch (ClientProtocolException e) {
				// e.printStackTrace();
				// } catch (IOException e) {
				// e.printStackTrace();
				// }
				File file = new File(MainActivity.this.fpath);
				MultipartEntity mpEntity = new MultipartEntity();
				ContentBody cbFile = new FileBody(file, "xls");
				mpEntity.addPart("userfile", cbFile);
				// mpEntity.addPart("bvs233", "vraelrulz");
				httppost.setEntity(mpEntity);
				System.out.println("executing request "
						+ httppost.getRequestLine());
				HttpResponse response;
				try {
					response = httpclient.execute(httppost);
					HttpEntity resEntity = response.getEntity();
				} catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// check the response and do what is required
			}
		};

		(new Thread(r)).start();
	}

	public void upload1(String filepath) throws IOException {
		fpath1 = filepath;
		Runnable r = new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub

				HttpClient httpclient = new DefaultHttpClient();
				httpclient.getParams().setParameter(
						CoreProtocolPNames.PROTOCOL_VERSION,
						HttpVersion.HTTP_1_1);
				HttpPost httppost1 = new HttpPost(
						"https://sprdatalink2014.poly.edu/Testing/file2.php");
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(
						2);
				File file1 = new File(MainActivity.this.fpath1);
				MultipartEntity mpEntity = new MultipartEntity();
				ContentBody cbFile1 = new FileBody(file1, "xls");
				mpEntity.addPart("userfile1", cbFile1);
				// mpEntity.addPart("bvs233", "vraelrulz");
				httppost1.setEntity(mpEntity);
				System.out.println("executing request "
						+ httppost1.getRequestLine());
				HttpResponse response;
				try {
					response = httpclient.execute(httppost1);
					HttpEntity resEntity = response.getEntity();
				} catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// check the response and do what is required
			}
		};

		(new Thread(r)).start();
	}

	public void upload2(String filepath) throws IOException {
		fpath2 = filepath;
		Runnable r = new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub

				HttpClient httpclient = new DefaultHttpClient();
				httpclient.getParams().setParameter(
						CoreProtocolPNames.PROTOCOL_VERSION,
						HttpVersion.HTTP_1_1);
				HttpPost httppost2 = new HttpPost(
						"https://sprdatalink2014.poly.edu/Testing/file3.php");
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(
						2);
				File file2 = new File(MainActivity.this.fpath2);
				MultipartEntity mpEntity = new MultipartEntity();
				ContentBody cbFile2 = new FileBody(file2, "xls");
				mpEntity.addPart("userfile2", cbFile2);
				// mpEntity.addPart("bvs233", "vraelrulz");
				httppost2.setEntity(mpEntity);
				System.out.println("executing request "
						+ httppost2.getRequestLine());
				HttpResponse response;
				try {
					response = httpclient.execute(httppost2);
					HttpEntity resEntity = response.getEntity();
				} catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// check the response and do what is required
			}
		};

		(new Thread(r)).start();
	}

	public void upload3(String filepath) throws IOException {
		fpath3 = filepath;
		Runnable r = new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub

				HttpClient httpclient = new DefaultHttpClient();
				httpclient.getParams().setParameter(
						CoreProtocolPNames.PROTOCOL_VERSION,
						HttpVersion.HTTP_1_1);
				HttpPost httppost3 = new HttpPost(
						"https://sprdatalink2014.poly.edu/Testing/file4.php");
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(
						2);
				File file3 = new File(MainActivity.this.fpath3);
				MultipartEntity mpEntity = new MultipartEntity();
				ContentBody cbFile3 = new FileBody(file3, "xls");
				mpEntity.addPart("userfile3", cbFile3);
				// mpEntity.addPart("bvs233", "vraelrulz");
				httppost3.setEntity(mpEntity);
				System.out.println("executing request "
						+ httppost3.getRequestLine());
				HttpResponse response;
				try {
					response = httpclient.execute(httppost3);
					HttpEntity resEntity = response.getEntity();
				} catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// check the response and do what is required
			}
		};

		(new Thread(r)).start();
	}

	public void upload4(String filepath) throws IOException {
		fpath4 = filepath;
		Runnable r = new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub

				HttpClient httpclient = new DefaultHttpClient();
				httpclient.getParams().setParameter(
						CoreProtocolPNames.PROTOCOL_VERSION,
						HttpVersion.HTTP_1_1);
				HttpPost httppost4 = new HttpPost(
						"https://sprdatalink2014.poly.edu/Testing/file5.php");
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(
						2);
				File file4 = new File(MainActivity.this.fpath4);
				MultipartEntity mpEntity = new MultipartEntity();
				ContentBody cbFile4 = new FileBody(file4, "xls");
				mpEntity.addPart("userfile4", cbFile4);
				// mpEntity.addPart("bvs233", "vraelrulz");
				httppost4.setEntity(mpEntity);
				System.out.println("executing request "
						+ httppost4.getRequestLine());
				HttpResponse response;
				try {
					response = httpclient.execute(httppost4);
					HttpEntity resEntity = response.getEntity();
				} catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// check the response and do what is required
			}
		};

		(new Thread(r)).start();
	}

	public void upload5(String filepath) throws IOException {
		fpath5 = filepath;
		Runnable r = new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub

				HttpClient httpclient = new DefaultHttpClient();
				httpclient.getParams().setParameter(
						CoreProtocolPNames.PROTOCOL_VERSION,
						HttpVersion.HTTP_1_1);
				HttpPost httppost5 = new HttpPost(
						"https://sprdatalink2014.poly.edu/Testing/file6.php");
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(
						2);
				File file5 = new File(MainActivity.this.fpath5);
				MultipartEntity mpEntity = new MultipartEntity();
				ContentBody cbFile5 = new FileBody(file5, "xls");
				mpEntity.addPart("userfile5", cbFile5);
				// mpEntity.addPart("bvs233", "vraelrulz");
				httppost5.setEntity(mpEntity);
				System.out.println("executing request "
						+ httppost5.getRequestLine());
				HttpResponse response;
				try {
					response = httpclient.execute(httppost5);
					HttpEntity resEntity = response.getEntity();
				} catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// check the response and do what is required
			}
		};

		(new Thread(r)).start();
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		// Toast.makeText(getApplicationContext(), "Single Tap Gesture",
		// 100).show(); return true;
		String strA = '\t' + "Single Tap Gesture" + '\n';
		try {
			out_stream.write(strA.getBytes());
		} catch (IOException i) {
			// TODO Auto-generated catch block
			i.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		// Toast.makeText(getApplicationContext(), "Scroll Gesture",
		// 100).show();
		String strA = '\t' + "Scroll Gesture" + '\n';
		try {
			out_stream.write(strA.getBytes());
		} catch (IOException i) {
			// TODO Auto-generated catch block
			i.printStackTrace();
		}
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		// Toast.makeText(getApplicationContext(), "Long Press Gesture",
		// 100).show();
		String strA = '\t' + "Long Press Gesture" + '\n';
		try {
			out_stream.write(strA.getBytes());
		} catch (IOException i) {
			// TODO Auto-generated catch block
			i.printStackTrace();
		}
	}

	@Override
	public boolean onDown(MotionEvent arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub

	}

}
