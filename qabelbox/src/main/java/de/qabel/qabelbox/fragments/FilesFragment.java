package de.qabel.qabelbox.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Map;

import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.adapter.FilesAdapter;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.helper.UIHelper;
import de.qabel.qabelbox.providers.DocumentIdParser;
import de.qabel.qabelbox.services.LocalBroadcastConstants;
import de.qabel.qabelbox.services.LocalQabelService;
import de.qabel.qabelbox.storage.BoxExternalFile;
import de.qabel.qabelbox.storage.BoxFile;
import de.qabel.qabelbox.storage.BoxFolder;
import de.qabel.qabelbox.storage.BoxNavigation;
import de.qabel.qabelbox.storage.BoxObject;
import de.qabel.qabelbox.storage.BoxUploadingFile;
import de.qabel.qabelbox.storage.BoxVolume;
import de.qabel.qabelbox.storage.StorageSearch;

public class FilesFragment extends BaseFragment {

	private static final String TAG = "FilesFragment";
	protected BoxNavigation boxNavigation;
	public RecyclerView filesListRecyclerView;
	protected FilesAdapter filesAdapter;
	private RecyclerView.LayoutManager recyclerViewLayoutManager;
	private boolean isLoading;
	private FilesListListener mListener;
	protected SwipeRefreshLayout swipeRefreshLayout;
	private FilesFragment self;
	private AsyncTask<Void, Void, Void> browseToTask;

	private MenuItem mSearchAction;
	private boolean isSearchOpened = false;
	private EditText edtSeach;
	protected BoxVolume mBoxVolume;
	private AsyncTask<String, Void, StorageSearch> searchTask;
	private StorageSearch mCachedStorageSearch;
	private DocumentIdParser documentIdParser;
	View mEmptyView;
	View mLoadingView;
	private LocalQabelService mService;

	public static FilesFragment newInstance(final BoxVolume boxVolume) {

		final FilesFragment filesFragment = new FilesFragment();
		fillFragmentData(boxVolume, filesFragment);
		return filesFragment;
	}

	protected static void fillFragmentData(final BoxVolume boxVolume, final FilesFragment filesFragment) {

		filesFragment.mBoxVolume = boxVolume;
		final FilesAdapter filesAdapter = new FilesAdapter(new ArrayList<BoxObject>());

		filesFragment.setAdapter(filesAdapter);

		new AsyncTask<Void, Void, Void>() {
			@Override
			protected void onPreExecute() {

				super.onPreExecute();
				filesFragment.setIsLoading(true);
			}

			@Override
			protected Void doInBackground(Void... params) {

				try {
					filesFragment.setBoxNavigation(boxVolume.navigate());
				} catch (QblStorageException e) {
					Log.w(TAG, "Cannot navigate to root. maybe first initialization", e);
					try {
						boxVolume.createIndex();
						filesFragment.setBoxNavigation(boxVolume.navigate());
					} catch (QblStorageException e1) {
						Log.e(TAG, "Creating a volume failed", e1);
						cancel(true);
						return null;
					}
				}
				filesFragment.fillAdapter(filesAdapter);
				return null;
			}

			@Override
			protected void onPostExecute(Void aVoid) {

				super.onPostExecute(aVoid);
				filesFragment.setIsLoading(false);
				filesAdapter.notifyDataSetChanged();
			}
		}.executeOnExecutor(serialExecutor);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(false);

			actionBar.setTitle(getTitle());
		}

		documentIdParser = new DocumentIdParser();

		mService = QabelBoxApplication.getInstance().getService();
		self = this;
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver,
				new IntentFilter(LocalBroadcastConstants.INTENT_UPLOAD_BROADCAST));
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			if (filesAdapter == null) {
				return;
			}
			String documentId = intent.getStringExtra(LocalBroadcastConstants.EXTRA_UPLOAD_DOCUMENT_ID);
			int uploadStatus = intent.getIntExtra(LocalBroadcastConstants.EXTRA_UPLOAD_STATUS, -1);

			switch (uploadStatus) {
				case LocalBroadcastConstants.UPLOAD_STATUS_NEW:
					Log.d(TAG, "Received new upload: " + documentId);
					fillAdapter(filesAdapter);
					filesAdapter.notifyDataSetChanged();
					break;
				case LocalBroadcastConstants.UPLOAD_STATUS_FINISHED:
					Log.d(TAG, "Received upload finished: " + documentId);
					fillAdapter(filesAdapter);
					filesAdapter.notifyDataSetChanged();
					break;
				case LocalBroadcastConstants.UPLOAD_STATUS_FAILED:
					Log.d(TAG, "Received upload failed: " + documentId);
					refresh();
					break;
			}
		}
	};

	@Override
	public void onDestroy() {

		super.onDestroy();
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
	}

	@Override
	public void onStart() {

		super.onStart();
		updateSubtitle();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.fragment_files, container, false);
		setupLoadingViews(view);
		swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefresh);
		swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {

				mListener.onDoRefresh(self, boxNavigation, filesAdapter);
			}
		});

		swipeRefreshLayout.post(new Runnable() {
			@Override
			public void run() {

				swipeRefreshLayout.setRefreshing(isLoading);
			}
		});
		filesListRecyclerView = (RecyclerView) view.findViewById(R.id.files_list);
		filesListRecyclerView.setHasFixedSize(true);

		recyclerViewLayoutManager = new LinearLayoutManager(view.getContext());
		filesListRecyclerView.setLayoutManager(recyclerViewLayoutManager);

		filesListRecyclerView.setAdapter(filesAdapter);

		filesListRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrolled(RecyclerView recyclerView, int dx, int dy) {

				super.onScrolled(recyclerView, dx, dy);
				int lastCompletelyVisibleItem = ((LinearLayoutManager) recyclerViewLayoutManager).findLastCompletelyVisibleItemPosition();
				int firstCompletelyVisibleItem = ((LinearLayoutManager) recyclerViewLayoutManager).findFirstCompletelyVisibleItemPosition();
				if (lastCompletelyVisibleItem == filesAdapter.getItemCount() - 1
						&& firstCompletelyVisibleItem > 0) {
					mListener.onScrolledToBottom(true);
				} else {
					mListener.onScrolledToBottom(false);
				}
			}
		});
		return view;
	}

	protected void setupLoadingViews(View view) {

		mEmptyView = view.findViewById(R.id.empty_view);
		mLoadingView = view.findViewById(R.id.loading_view);
		final ProgressBar pg = (ProgressBar) view.findViewById(R.id.pb_firstloading);
		pg.setIndeterminate(true);
		pg.setEnabled(true);
		if (filesAdapter != null)
			filesAdapter.setEmptyView(mEmptyView, mLoadingView);
	}

	@Override
	public void onAttach(Activity activity) {

		super.onAttach(activity);
		try {
			mListener = (FilesListListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement FilesListListener");
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

		menu.clear();
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.ab_files, menu);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {

		mSearchAction = menu.findItem(R.id.action_search);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// handle item selection
		switch (item.getItemId()) {
			case R.id.action_search:
				if (!isSearchRunning()) {
					handleMenuSearch();
				} else if (isSearchOpened) {
					removeSearchInActionbar(actionBar);
				}
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private boolean isSearchRunning() {

		if (isSearchOpened) {

			return true;
		}
		return searchTask != null && ((!searchTask.isCancelled() && searchTask.getStatus() != AsyncTask.Status.FINISHED));
	}

	/**
	 * handle click on search icon
	 */
	private void handleMenuSearch() {

		if (isSearchOpened) {
			removeSearchInActionbar(actionBar);
		} else {
			openSearchInActionBar(actionBar);
		}
	}

	/**
	 * setup the actionbar to show a input dialog for search keyword
	 *
	 * @param action
	 */
	private void openSearchInActionBar(final ActionBar action) {

		action.setDisplayShowCustomEnabled(true);
		action.setCustomView(R.layout.ab_search_field);
		action.setDisplayShowTitleEnabled(false);

		edtSeach = (EditText) action.getCustomView().findViewById(R.id.edtSearch);

		//add editor action listener
		edtSeach.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

				if (actionId == EditorInfo.IME_ACTION_SEARCH) {
					String text = edtSeach.getText().toString();
					removeSearchInActionbar(action);
					startSearch(text);
					return true;
				}
				return false;
			}
		});

		edtSeach.requestFocus();

		//open keyboard
		InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.showSoftInput(edtSeach, InputMethodManager.SHOW_IMPLICIT);
		mActivity.fab.hide();
		mSearchAction.setIcon(R.drawable.ic_ab_close);
		isSearchOpened = true;
	}

	/**
	 * restore the actionbar
	 *
	 * @param action
	 */
	private void removeSearchInActionbar(ActionBar action) {

		action.setDisplayShowCustomEnabled(false);
		action.setDisplayShowTitleEnabled(true);

		//hides the keyboard
		InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(edtSeach.getWindowToken(), 0);

		imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.RESULT_HIDDEN);
		mSearchAction.setIcon(R.drawable.ic_ab_search);
		action.setTitle(getTitle());
		isSearchOpened = false;
		mActivity.fab.show();
	}

	@Override
	public void onPause() {

		if (isSearchOpened) {
			removeSearchInActionbar(actionBar);
		}
		super.onPause();
	}

	/**
	 * start search
	 *
	 * @param searchText
	 */
	private void startSearch(final String searchText) {

		cancelSearchTask();
		searchTask = new AsyncTask<String, Void, StorageSearch>() {

			@Override
			protected void onPreExecute() {

				super.onPreExecute();
				setIsLoading(true);
			}

			@Override
			protected void onCancelled(StorageSearch storageSearch) {

				setIsLoading(false);
				super.onCancelled(storageSearch);
			}

			@Override
			protected void onPostExecute(StorageSearch storageSearch) {

				setIsLoading(false);

				//check if files found
				if (storageSearch == null || storageSearch.filterOnlyFiles().getResults().size() == 0) {
					Toast.makeText(getActivity(), R.string.no_entrys_found, Toast.LENGTH_SHORT).show();
					return;
				}
				if (!mActivity.isFinishing() && !searchTask.isCancelled()) {
					boolean needRefresh = mCachedStorageSearch != null;
					try {
						mCachedStorageSearch = storageSearch.clone();
					} catch (CloneNotSupportedException e) {
						e.printStackTrace();
					}

					FilesSearchResultFragment fragment = FilesSearchResultFragment.newInstance(mCachedStorageSearch, searchText, needRefresh);
					mActivity.toggle.setDrawerIndicatorEnabled(false);
					getFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment, FilesSearchResultFragment.TAG).addToBackStack(null).commit();
				}
			}

			@Override
			protected StorageSearch doInBackground(String... params) {

				try {
					if (mCachedStorageSearch != null && mCachedStorageSearch.getResults().size() > 0) {
						return mCachedStorageSearch;
					}

					return new StorageSearch(mBoxVolume.navigate());
				} catch (QblStorageException e) {
					e.printStackTrace();
				}

				return null;
			}
		};
		searchTask.executeOnExecutor(serialExecutor);
	}

	private void cancelSearchTask() {

		if (searchTask != null) {
			searchTask.cancel(true);
		}
	}

	/**
	 * Sets visibility of loading spinner. Visibility is stored if method is invoked
	 * before onCreateView() has completed.
	 *
	 * @param isLoading
	 */
	public void setIsLoading(final boolean isLoading) {

		this.isLoading = isLoading;
		if (swipeRefreshLayout == null) {
			return;
		}
		if (!isLoading) {
			mLoadingView.setVisibility(View.GONE);
		}
		swipeRefreshLayout.post(new Runnable() {
			@Override
			public void run() {

				swipeRefreshLayout.setRefreshing(isLoading);
			}
		});
	}

	public void setAdapter(FilesAdapter adapter) {

		filesAdapter = adapter;
		filesAdapter.setEmptyView(mEmptyView, mLoadingView);
	}

	public FilesAdapter getFilesAdapter() {

		return filesAdapter;
	}

	public void setOnItemClickListener(FilesAdapter.OnItemClickListener onItemClickListener) {

		filesAdapter.setOnItemClickListener(onItemClickListener);
	}

	@Override
	public boolean isFabNeeded() {

		return true;
	}

	protected void setBoxNavigation(BoxNavigation boxNavigation) {

		this.boxNavigation = boxNavigation;
	}

	public BoxNavigation getBoxNavigation() {

		return boxNavigation;
	}

	@Override
	public String getTitle() {

		return getString(R.string.headline_files);
	}

	/**
	 * handle back pressed
	 *
	 * @return true if back handled
	 */
	public boolean handleBackPressed() {

		if (isSearchOpened) {
			removeSearchInActionbar(actionBar);
			return true;
		}
		if (searchTask != null && ((!searchTask.isCancelled() && searchTask.getStatus() != AsyncTask.Status.FINISHED))) {
			cancelSearchTask();
			return true;
		}

		return false;
	}

	public BoxVolume getBoxVolume() {

		return mBoxVolume;
	}

	public void setCachedSearchResult(StorageSearch searchResult) {

		mCachedStorageSearch = searchResult;
	}

	public void unshare(final BoxFile boxObject) {
		new AsyncTask<Void, Void, Boolean>() {
			public AlertDialog wait;

			@Override
			protected void onPreExecute() {
				wait=UIHelper.showWaitMessage(mActivity,R.string.dialog_headline_info,R.string.message_revoke_share,false);
			}



			@Override
			protected Boolean doInBackground(Void... params) {
				boolean ret = getBoxNavigation().removeFileMetadata(boxObject);
				try {
					getBoxNavigation().commit();
				} catch (QblStorageException e) {
					e.printStackTrace();
					return false;
				}
				return ret;


			}
			@Override
			protected void onPostExecute(Boolean success) {
				if (success) {
					refresh();
					Toast.makeText(mActivity, R.string.message_unshare_successfull, Toast.LENGTH_SHORT).show();
				} else {
					UIHelper.showDialogMessage(mActivity, R.string.dialog_headline_warning, R.string.message_unshare_not_successfull, Toast.LENGTH_SHORT);

				}
				wait.dismiss();

			}
		}.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);

	}

	public void delete(final BoxObject boxObject) {

		new AlertDialog.Builder(mActivity)
				.setTitle(R.string.confirm_delete_title)
				.setMessage(String.format(
						getResources().getString(R.string.confirm_delete_message), boxObject.name))
				.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {

						new AsyncTask<Void, Void, Void>() {
							@Override
							protected void onCancelled() {

								setIsLoading(false);
							}

							@Override
							protected void onPreExecute() {

								setIsLoading(true);
							}

							@Override
							protected Void doInBackground(Void... params) {

								try {
									if (boxObject instanceof BoxExternalFile) {
										getBoxNavigation().detachExternal(boxObject.name);
									} else {
										getBoxNavigation().delete(boxObject);
									}
									getBoxNavigation().commit();
								} catch (QblStorageException e) {
									Log.e(TAG, "Cannot delete " + boxObject.name);
								}
								return null;
							}

							@Override
							protected void onPostExecute(Void aVoid) {

								refresh();
							}
						}.execute();
					}
				})
				.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {

						showAbortMessage();
					}
				}).create().show();
	}

	public void refresh() {

		if (boxNavigation == null) {
			Log.e(TAG, "Refresh failed because the boxNavigation object is null");
			return;
		}
		AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {

				try {
					boxNavigation.reload();
					mService.getCachedFinishedUploads().clear();
					loadBoxObjectsToAdapter(boxNavigation, filesAdapter);
				} catch (QblStorageException e) {
					Log.e(TAG, "refresh failed", e);
				}
				return null;
			}

			@Override
			protected void onCancelled() {

				setIsLoading(true);
				showAbortMessage();
			}

			@Override
			protected void onPreExecute() {

				setIsLoading(true);
			}

			@Override
			protected void onPostExecute(Void aVoid) {

				super.onPostExecute(aVoid);

				filesAdapter.sort();
				filesAdapter.notifyDataSetChanged();

				setIsLoading(false);
			}
		};
		asyncTask.execute();
	}

	private void showAbortMessage() {

		Toast.makeText(mActivity, R.string.aborted,
				Toast.LENGTH_SHORT).show();
	}

	public interface FilesListListener {

		void onScrolledToBottom(boolean scrolledToBottom);

		void onExport(BoxNavigation boxNavigation, BoxObject object);

		void onDoRefresh(FilesFragment filesFragment, BoxNavigation boxNavigation, FilesAdapter filesAdapter);
	}

	public boolean browseToParent() {

		cancelBrowseToTask();

		if (boxNavigation == null || !boxNavigation.hasParent()) {
			return false;
		}

		browseToTask = new AsyncTask<Void, Void, Void>() {
			@Override
			protected void onPreExecute() {

				super.onPreExecute();
				preBrowseTo();
			}

			@Override
			protected Void doInBackground(Void... voids) {

				waitForBoxNavigation();
				try {
					boxNavigation.navigateToParent();
					fillAdapter(filesAdapter);
				} catch (QblStorageException e) {
					Log.d(TAG, "browseTo failed", e);
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void aVoid) {

				super.onPostExecute(aVoid);
				setIsLoading(false);
				updateSubtitle();
				filesAdapter.notifyDataSetChanged();
			}
		};
		browseToTask.executeOnExecutor(serialExecutor);
		return true;
	}

	@Override
	public void updateSubtitle() {

		String path = boxNavigation != null ? boxNavigation.getPath() : "";
		if (path.equals("/")) {
			path = null;
		}
		if (actionBar != null)
			actionBar.setSubtitle(path);
	}

	private void preBrowseTo() {

		setIsLoading(true);
	}

	protected void fillAdapter(FilesAdapter filesAdapter) {

		if (filesAdapter == null || boxNavigation == null) {
			return;
		}
		try {
			loadBoxObjectsToAdapter(boxNavigation, filesAdapter);
			insertCachedFinishedUploads(filesAdapter);
			insertPendingUploads(filesAdapter);
			filesAdapter.sort();
		} catch (QblStorageException e) {
			Log.e(TAG, "fillAdapter failed", e);
		} catch (Exception e) {
			//catch all other, if something going wrong that app would crashed on started and user have no way to change this
			Log.e(TAG, "fillAdapter failed", e);
		}
	}

	private void insertPendingUploads(FilesAdapter filesAdapter) {

		if (mService != null && mService.getPendingUploads() != null) {
			Map<String, BoxUploadingFile> uploadsInPath = mService.getPendingUploads().get(boxNavigation.getPath());
			if (uploadsInPath != null) {
				for (BoxUploadingFile boxUploadingFile : uploadsInPath.values()) {
					filesAdapter.remove(boxUploadingFile.name);

					filesAdapter.add(boxUploadingFile);
				}
			}
		}
	}

	private void insertCachedFinishedUploads(FilesAdapter filesAdapter) {

		if (mService != null && mService.getCachedFinishedUploads() != null) {
			Map<String, BoxFile> cachedFiles = mService.getCachedFinishedUploads().get(boxNavigation.getPath());
			if (cachedFiles != null) {
				for (BoxFile boxFile : cachedFiles.values()) {
					filesAdapter.remove(boxFile.name);
					filesAdapter.add(boxFile);
				}
			}
		}
	}

	private void loadBoxObjectsToAdapter(BoxNavigation boxNavigation, FilesAdapter filesAdapter) throws QblStorageException, ArrayIndexOutOfBoundsException {

		filesAdapter.clear();
		for (BoxFolder boxFolder : boxNavigation.listFolders()) {
			Log.d(TAG, "Adding folder: " + boxFolder.name);
			filesAdapter.add(boxFolder);
		}
		for (BoxObject boxExternal : boxNavigation.listExternals()) {
			Log.d(TAG, "Adding external: " + boxExternal.name);
			filesAdapter.add(boxExternal);
		}
		for (BoxFile boxFile : boxNavigation.listFiles()) {
			Log.d(TAG, "Adding file: " + boxFile.name);
			filesAdapter.add(boxFile);
		}
	}

	private void waitForBoxNavigation() {

		while (boxNavigation == null) {
			Log.d(TAG, "waiting for BoxNavigation");
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				return;
			}
		}
	}

	@Override
	public boolean supportSubtitle() {

		return true;
	}

	public void browseTo(final BoxFolder navigateTo) {

		Log.d(TAG, "Browsing to " + navigateTo.name);
		cancelBrowseToTask();
		browseToTask = new AsyncTask<Void, Void, Void>() {
			@Override
			protected void onPreExecute() {

				super.onPreExecute();
				preBrowseTo();
			}

			@Override
			protected Void doInBackground(Void... voids) {

				waitForBoxNavigation();
				try {
					boxNavigation.navigate(navigateTo);
					fillAdapter(filesAdapter);
				} catch (QblStorageException e) {
					Log.e(TAG, "browseTo failed", e);
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void aVoid) {

				super.onPostExecute(aVoid);
				setIsLoading(false);
				updateSubtitle();
				filesAdapter.notifyDataSetChanged();
				browseToTask = null;
			}

			@Override
			protected void onCancelled() {

				super.onCancelled();
				setIsLoading(false);
				browseToTask = null;
				Toast.makeText(getActivity(), R.string.aborted,
						Toast.LENGTH_SHORT).show();
			}
		};
		browseToTask.executeOnExecutor(serialExecutor);
	}

	private void cancelBrowseToTask() {

		if (browseToTask != null) {
			Log.d(TAG, "Found a running browseToTask");
			browseToTask.cancel(true);
			Log.d(TAG, "Canceled browserToTask");
		}
	}
}
