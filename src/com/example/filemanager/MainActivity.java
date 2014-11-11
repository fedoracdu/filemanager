package com.example.filemanager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.DrawerLayout;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity implements
		NavigationDrawerFragment.NavigationDrawerCallbacks {

	/**
	 * Fragment managing the behaviors, interactions and presentation of the
	 * navigation drawer.
	 */
	private NavigationDrawerFragment mNavigationDrawerFragment;
	
	/**
	 * Used to store the last screen title. For use in
	 * {@link #restoreActionBar()}.
	 */
	private CharSequence mTitle;

	private ListView mListView;
	
	private ArrayList<HashMap<String, Object>> list;
	
	private ArrayList<HashMap<String, Integer>> positionList;
	
	private SparseArray<String> absPathList;
	
	private SimpleAdapter itemAdapter;
	
	private File homePath;
	
	private File currentPath;
	
	private Boolean checkBoxVisible;
	
	private Boolean displayHideFile;
	
	private SparseArray<Boolean> selectStatus;
	
	private SharedPreferences sharedPreferences;
	
	private ProgressDialog progressDialog;
	
	private static String hideFileKey = "hidefile";
	
	private static final int INIT_MAGIC_MSG = 1;
	private static final int UPDATE_LV_MSG = 2;
	
	public native int delFile(String fileName);
	public native int initMagic(String fileName);
	public native String getFileType(String fileName);
	public native int uninitMagic();
	static {
		System.loadLibrary("file-operation");
	}
	
	private void createMagicFile()
	{
		String tmp = homePath.getPath() + "/magicfile";
		File file = new File(tmp);
		if (false == file.exists())
			file.mkdir();
		tmp += "/magic.mgc";
		file = new File(tmp);
		if (file.exists())
			return ;
		
		try {
			InputStream is = getAssets().open("magic.mgc");
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			byte[] buffer = new byte[512];
			while (true) {
				int len = is.read(buffer);
				if (-1 == len)
					break;
				fileOutputStream.write(buffer);
			}
			is.close();
			fileOutputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg)
		{
			switch (msg.what) {
			case INIT_MAGIC_MSG:
				createMagicFile();
				initMagic(homePath.getPath() + "/magicfile" + "/magic.mgc");
				updateListByFile(currentPath);
				itemAdapter.notifyDataSetChanged();
				break;
			
			case UPDATE_LV_MSG:
				progressDialog.dismiss();
				
				itemAdapter.notifyDataSetChanged();
				
				Bundle bundle = msg.getData();
				String path = bundle.getString("path");
				for (int i = 0; i < positionList.size(); i++) {
					HashMap<String, Integer> map = positionList.get(i);
					if (map.containsKey(path)) {
						mListView.setSelectionFromTop(map.get(path), 0);
					}
				}
				break;
				
			default:
				break;
			}
		}
	};
	
	public class myAdapter extends SimpleAdapter
	{	
		public myAdapter(Context context, List<? extends Map<String, ?>> data,
				int resource, String[] from, int[] to) {
			super(context, data, resource, from, to);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			
			view =  super.getView(position, convertView, parent);
			
			CheckBox checkBox = (CheckBox)view.findViewById(R.id.checkBox1);
			if (checkBoxVisible) {
				if (selectStatus.get(position))
					checkBox.setChecked(true);
				else {
					checkBox.setChecked(false);
				}
				checkBox.setVisibility(CheckBox.VISIBLE);
			} else {
				checkBox.setVisibility(CheckBox.INVISIBLE);
			}
			
			return view;
		}
		
	}
	
	public class updateListViewThread extends Thread
	{
		File file;
		
		public updateListViewThread(File file)
		{
			this.file = file;
		}
		
		@Override
		public void run()
		{
			updateListByFile(file);
			
			Message msg = new Message();
			msg.what = UPDATE_LV_MSG;
			
			Bundle bundle = new Bundle();
			bundle.putSerializable("path", file.getPath());
			msg.setData(bundle);
			mHandler.sendMessage(msg);
		}
	}
	
	private void updateListView(File file)
	{
		if (null == progressDialog) {
			progressDialog = new ProgressDialog(MainActivity.this);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progressDialog.setTitle("载入中");
			progressDialog.setMessage("文件类型检测中...");
			progressDialog.setCancelable(false);
		}
		
		if (!file.isDirectory())
			return ;
		
		File[] files = file.listFiles();
		if (null == files) {
			Toast.makeText(getApplicationContext(), "directory " + file.getPath() + " permission denied", Toast.LENGTH_SHORT).show();
			return ;
		}
		
		if (files.length < 5) {
			updateListByFile(file);
			itemAdapter.notifyDataSetChanged();
			
			for (int i = 0; i < positionList.size(); i++) {
				HashMap<String, Integer> map = positionList.get(i);
				String path = file.getPath();
				if (map.containsKey(path)) {
					mListView.setSelectionFromTop(map.get(path), 0);
				}
			}
			
			return ;
		}
		
		progressDialog.show();
		
		updateListViewThread thread = new updateListViewThread(file);
		thread.start();
	}
	
	@SuppressLint("UseSparseArrays")
	public void init()
	{
		String[] 	keysStrings = {"image", "text"};
		int[] 		ids = {R.id.image1, R.id.text1};
		HashMap<String, Object>	map = null;
		
		mTitle = getString(R.string.home_path);
		checkBoxVisible = false;
		
		list = new ArrayList<HashMap<String, Object>> ();
		positionList = new ArrayList<HashMap<String,Integer>>();
		absPathList = new SparseArray<String>();
		
		sharedPreferences = getPreferences(MODE_PRIVATE);
		displayHideFile = sharedPreferences.getBoolean(hideFileKey, false);
		
		mListView = (ListView)findViewById(R.id.mainlv);
		
		try {
			int	idx = 0;
			homePath = Environment.getExternalStorageDirectory();
			File[] files = homePath.listFiles();
			for (int lp = 0; lp < files.length; lp++) {
				map = new HashMap<String, Object>();
				String[] pathSplit = files[lp].getPath().split("/");
				String fileName = pathSplit[pathSplit.length - 1];
				if (!displayHideFile && fileName.startsWith("."))
					continue;
				
				map.put("text", fileName);
				if (files[lp].isDirectory()) {
					map.put("image", R.drawable.ic_folder);
				} else {
					map.put("image", R.drawable.ic_text);
				}
				list.add(map);
				absPathList.put(idx++, files[lp].getPath());
			}
		} catch (Exception exception){
			map = new HashMap<String, Object>();
			map.put("text", "can NOT get homePath");
			map.put("image", R.drawable.ic_text);
			list.add(map);
			absPathList.put(0, "homePath error");
		}
		
		selectStatus = new SparseArray<Boolean>();
		
		for (int i = 0; i < list.size(); i++)
			selectStatus.put(i, false);
		
		currentPath = homePath;
		
		itemAdapter = new myAdapter(this, list, R.layout.display_listitem, keysStrings, ids);
		
		mListView.setAdapter(itemAdapter);
		mListView.setDivider(null);
		
		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				if (checkBoxVisible) {
					if (selectStatus.get(position))
						selectStatus.put(position, false);
					else
						selectStatus.put(position, true);
					itemAdapter.notifyDataSetChanged();
				} else {
					File file = new File(absPathList.get(position));
					
					updateListView(file);
				}
			}
		});
		
		mListView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				selectStatus.put(position, true);
				displayCheckBox(true);
				
				return true;
			}
			
		});
		
		mListView.setOnScrollListener(new OnScrollListener() {
			
			@Override
			public void onScrollStateChanged(AbsListView arg0, int arg1) {
				int position = arg0.getFirstVisiblePosition();
				HashMap<String, Integer> map = null;
				for (int i = 0; i < positionList.size(); i++) {
					map = positionList.get(i);
					if (map.containsKey(currentPath.getPath())) {
						map.put(currentPath.getPath(), position);
						return ;
					}
				}
				map = new HashMap<String, Integer>();
				map.put(currentPath.getPath(), position);
				positionList.add(map);
			}
			
			@Override
			public void onScroll(AbsListView arg0, int arg1, int arg2, int arg3) {
			}
		});
		
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
	
		mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager()
				.findFragmentById(R.id.navigation_drawer);

		// Set up the drawer.
		mNavigationDrawerFragment.setUp(R.id.navigation_drawer,
				(DrawerLayout) findViewById(R.id.drawer_layout));
		
		init();
		
		new Thread() {

			@Override
			public void run() {
				Message msg = new Message();
				msg.what = INIT_MAGIC_MSG;
				
				mHandler.sendMessage(msg);
			}
		}.start();
	}

	@Override
	public void onBackPressed() {
		if (checkBoxVisible) {
			displayCheckBox(false);
		} else {
			String path = currentPath.getPath();
			String[] split = path.split("/");
			if (split.length < 2) {
				return ;
			}
			
			if (2 == split.length)
				currentPath = new File("/");
			else {
				String newPath = path.substring(0, path.length() - split[split.length - 1].length() - 1);
				currentPath = new File(newPath);
			}
				
			updateListView(currentPath);
		}
	}

	public void onSectionAttached(int number) {
		switch (number) {
		case 1:
			mTitle = getString(R.string.title_section1);
			break;
		case 2:
			mTitle = getString(R.string.title_section2);
			break;
		}
	}

	public void restoreActionBar() {
		ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setTitle(mTitle);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (!mNavigationDrawerFragment.isDrawerOpen()) {
			getMenuInflater().inflate(R.menu.main, menu);
			restoreActionBar();
			return true;
		}
		return super.onCreateOptionsMenu(menu);
	}

	@SuppressLint("SimpleDateFormat")
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		LayoutInflater inflater;
		View layout;
		
		switch (id) {
			case R.id.action_settings:
			
				inflater = getLayoutInflater();
				layout = inflater.inflate(R.layout.settings, null);
				final CheckBox checkBox = (CheckBox)layout.findViewById(R.id.display_hidefile_checkbox);
				checkBox.setChecked(displayHideFile);
				checkBox.setOnClickListener(new OnClickListener() {
				
					@Override
					public void onClick(View arg0) {
						if (checkBox.isChecked())
							displayHideFile = true;
						else {
							displayHideFile = false;
						}
						updateListView(currentPath);
						
						Editor editor = sharedPreferences.edit();
						editor.putBoolean(hideFileKey, displayHideFile);
						editor.commit();
					}
				});
			
				new AlertDialog.Builder(this).setTitle(R.string.action_settings).setView(layout).show();
				
				break;
			
			case R.id.delete:
				
				for (int i = 0; i < absPathList.size(); i++)
					if (selectStatus.get(i)) {
						if (0 != delFile(absPathList.get(i))) {
							Toast.makeText(getApplicationContext(), absPathList.get(i) + " delete fail", Toast.LENGTH_SHORT).show();
						}
					}
				
				updateListView(currentPath);
				
				break;
			
			case R.id.exit:
				uninitMagic();
				super.onBackPressed();
				
				break;
			
			case R.id.property:
				
				inflater = getLayoutInflater();
				layout = inflater.inflate(R.layout.property, null);
				final TextView absPathTv = (TextView)layout.findViewById(R.id.abs_path_tv);
				TextView fileSizeTv = (TextView)layout.findViewById(R.id.file_size_tv);
				TextView lastModifiedTv = (TextView)layout.findViewById(R.id.file_last_modified_tv);
				TextView fileTypeTView = (TextView)layout.findViewById(R.id.file_type_tv);
				Button copyButton = (Button)layout.findViewById(R.id.copy_abs_path_bt);
				Boolean fileChoosen = false;
				
				for (int i = 0; i < absPathList.size(); i++) {
					if (selectStatus.get(i)) {
						fileChoosen = true;
						
						absPathTv.setText("绝对路径: " + absPathList.get(i));
						try {
							File file = new File(absPathList.get(i));
							long fileSize = file.length();
							long lastModified = file.lastModified();
							if (fileSize < 1024) {
								fileSizeTv.setText("文件大小: " + fileSize + "字节");
							} else if (fileSize < 1024 * 1024) {
								float tmp = fileSize / 1024.0f;
								fileSizeTv.setText("文件大小: " + (float)(Math.round(tmp * 100)) / 100 + "KB");
							} else {
								float tmp = fileSize / 1024.0f / 1024;
								fileSizeTv.setText("文件大小: " + (float)(Math.round(tmp * 100)) / 100 + "MB");
							}
							SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						
							lastModifiedTv.setText("最后修改时间: " + format.format(lastModified));
							fileTypeTView.setText("文件类型: " + getFileType(absPathList.get(i)));
						} catch (Exception e) {
							Toast.makeText(getApplicationContext(), "获取属性失败",Toast.LENGTH_SHORT).show();
						}
						break;
					}
				}
			
				if (!fileChoosen)
					break;
				
				copyButton.setOnClickListener(new OnClickListener() {
					
					@SuppressWarnings("deprecation")
					@Override
					public void onClick(View arg0) {
						String path = absPathTv.getText().toString();
						path = path.replace("绝对路径: ", "");
						ClipboardManager clipboardManager = (ClipboardManager)getApplication().getSystemService(Context.CLIPBOARD_SERVICE);
						clipboardManager.setText(path);
					}
				});
				
				new AlertDialog.Builder(this).setTitle(R.string.property).setView(layout).show();
				
				break;
			
			case R.id.refresh:
				
				updateListView(currentPath);
				
				break;
			
			default:
				break;
		}
		
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onDestroy() {
		uninitMagic();
		super.onDestroy();
	}
	@Override
	public void onNavigationDrawerItemSelected(int position) {
		switch (position) {
		case 0:
			mTitle = getString(R.string.home_path);
			break;

		case 1:
			mTitle = "FTP";
			LayoutInflater inflater = getLayoutInflater();
			View layout = inflater.inflate(R.layout.ftplogin, null);
			
			new AlertDialog.Builder(this).setTitle(R.string.title_section2).setView(layout).show();
			break;
			
		default:
			break;
		}
	}
	
	public void updateListByFile(File path)
	{
		list.clear();
		absPathList.clear();
		selectStatus.clear();
		
		currentPath = path;
		
		int		idx = 0;
		File[] files = path.listFiles();
		
		if (null == files) {
			return ;
		}
		
		for (int lp = 0; lp < files.length; lp++) {
			String[] pathSplit = files[lp].getPath().split("/");
			HashMap<String, Object> map = new HashMap<String, Object>();
			String fileName = pathSplit[pathSplit.length - 1];
			if (!displayHideFile && fileName.startsWith("."))
				continue;
				
			map.put("text", fileName);
			if (files[lp].isDirectory()) {
				map.put("image", R.drawable.ic_folder);
			} else {
				String fileType = getFileType(files[lp].getPath());
				if (fileType.contains("Audio"))
					map.put("image", R.drawable.ic_audio);
				else if (fileType.contains("JPEG") || fileType.contains("JPG"))
					map.put("image", R.drawable.ic_jpeg);
				else if (fileType.contains("PNG"))
					map.put("image", R.drawable.ic_png);
				else
					map.put("image", R.drawable.ic_text);
			}
			list.add(map);
			absPathList.put(idx, files[lp].getPath());
			selectStatus.put(idx++, false);
		}
		
		return ;
	}
	
	public void updateListByString(String value)
	{
		if (null == value) {
			return ;
		}
		
		list.clear();
		absPathList.clear();
		
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("text", value);
		map.put("image", R.drawable.ic_text);
		list.add(map);
		absPathList.put(0, value);
		selectStatus.put(0, false);
		
		itemAdapter.notifyDataSetChanged();
		
		return ;
	}
	
	
	public void displayCheckBox(Boolean visible)
	{
		checkBoxVisible = visible;
		
		if (false == visible) {
			for (int i = 0; i < list.size(); i++)
				selectStatus.put(i, false);
		}
		itemAdapter.notifyDataSetChanged();
	}
}