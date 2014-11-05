package com.example.filemanager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.widget.DrawerLayout;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AbsListView;
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
	
	private static String hideFileKey = "hidefile";
	
	static {
		System.loadLibrary("file-operation");
	}
	
	public native int delFile(String fileName);
	
	public class myAdapter extends SimpleAdapter
	{	
		public myAdapter(Context context, List<? extends Map<String, ?>> data,
				int resource, String[] from, int[] to) {
			super(context, data, resource, from, to);
			// TODO Auto-generated constructor stub
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
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
					updateListByFile(file);
				}
			}
		});
		
		mListView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				
				selectStatus.put(position, true);
				displayCheckBox(true);
				
				return true;
			}
			
		});
		
		mListView.setOnScrollListener(new OnScrollListener() {
			
			@Override
			public void onScrollStateChanged(AbsListView arg0, int arg1) {
				// TODO Auto-generated method stub
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
				// TODO Auto-generated method stub
				
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
				
			updateListByFile(currentPath);
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
			// Only show items in the action bar relevant to this screen
			// if the drawer is not showing. Otherwise, let the drawer
			// decide what to show in the action bar.
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
						updateListByFile(currentPath);
						
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
				
				updateListByFile(currentPath);
				
				break;
			
			case R.id.exit:
				super.onBackPressed();
				
				break;
			
			case R.id.property:
				
				inflater = getLayoutInflater();
				layout = inflater.inflate(R.layout.property, null);
				final TextView absPathTv = (TextView)layout.findViewById(R.id.abs_path_tv);
				TextView fileSizeTv = (TextView)layout.findViewById(R.id.file_size_tv);
				TextView lastModifiedTv = (TextView)layout.findViewById(R.id.file_last_modified_tv);
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
						} catch (Exception e) {
						// TODO: handle exception
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
						// TODO Auto-generated method stub
						String path = absPathTv.getText().toString();
						path = path.replace("绝对路径: ", "");
						ClipboardManager clipboardManager = (ClipboardManager)getApplication().getSystemService(Context.CLIPBOARD_SERVICE);
						clipboardManager.setText(path);
					}
				});
				
				new AlertDialog.Builder(this).setTitle(R.string.property).setView(layout).show();
				
				break;
			
			case R.id.refresh:
				
				updateListByFile(currentPath);
				
				break;
			
			default:
				break;
		}
		
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onNavigationDrawerItemSelected(int position) {
		switch (position) {
		case 0:
			mTitle = getString(R.string.home_path);
			updateListByFile(currentPath);
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
		if (null == path || !path.exists() || !path.isDirectory())
			return ;
		
		list.clear();
		absPathList.clear();
		selectStatus.clear();
		
		currentPath = path;
		
		try {
			int		idx = 0;
			File[] files = path.listFiles();
				
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
					map.put("image", R.drawable.ic_text);
				}
				list.add(map);
				absPathList.put(idx, files[lp].getPath());
				selectStatus.put(idx++, false);
			}
		} catch (Exception e) {
			Toast.makeText(getApplicationContext(), path.getPath() + " permission denied", Toast.LENGTH_SHORT).show();
		}
		
		itemAdapter.notifyDataSetChanged();
		
		for (int i = 0; i < positionList.size(); i++) {
			HashMap<String, Integer> map = positionList.get(i);
			if (map.containsKey(path.getPath())) {
				mListView.setSelectionFromTop(map.get(path.getPath()), 0);
			}
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