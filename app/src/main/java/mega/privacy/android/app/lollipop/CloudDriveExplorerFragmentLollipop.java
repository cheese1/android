package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.CustomizedGridLayoutManager;
import mega.privacy.android.app.components.NewGridRecyclerView;
import mega.privacy.android.app.components.NewHeaderItemDecoration;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.lollipop.adapters.MegaExplorerLollipopAdapter;
import mega.privacy.android.app.lollipop.adapters.MegaNodeAdapter;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;


public class CloudDriveExplorerFragmentLollipop extends Fragment implements OnClickListener{

	Context context;
	MegaApiAndroid megaApi;
	ArrayList<MegaNode> nodes;
	ArrayList<MegaNode> searchNodes = null;
	DisplayMetrics metrics;

	public long parentHandle = -1;
	
	MegaExplorerLollipopAdapter adapter;
	
	int modeCloud;
	boolean selectFile=false;
	MegaPreferences prefs;
	DatabaseHandler dbH;
	public ActionMode actionMode;
	
//	public String name;
	
//	boolean first = false;
//	private boolean folderSelected = false;
	LinearLayout optionsBar;
	RecyclerView recyclerView;
	LinearLayoutManager mLayoutManager;
	CustomizedGridLayoutManager gridLayoutManager;

	ImageView emptyImageView;
	LinearLayout emptyTextView;
	TextView emptyTextViewFirst;

	TextView contentText;
	Button optionButton;
	Button cancelButton;
	View separator;

	ArrayList<Long> nodeHandleMoveCopy;

	Stack<Integer> lastPositionStack;

	Handler handler;

	int order;

	public NewHeaderItemDecoration headerItemDecoration;

	public void activateActionMode(){
		log("activateActionMode");
		if (!adapter.isMultipleSelect()){
			adapter.setMultipleSelect(true);
			actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());

			if(isMultiselect()) {
				activateButton(true);
			}
		}
	}

	private class ActionBarCallBack implements ActionMode.Callback {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			log("onActionItemClicked");
			List<MegaNode> documents = adapter.getSelectedNodes();

			switch(item.getItemId()){

				case R.id.cab_menu_select_all:{
					selectAll();
					break;
				}
				case R.id.cab_menu_unselect_all:{
					clearSelections();
					hideMultipleSelect();
					break;
				}
			}
			return false;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			log("onCreateActionMode");
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.file_explorer_multiaction, menu);
			Util.changeStatusBarColorActionMode(context, ((FileExplorerActivityLollipop) context).getWindow(), handler, 1);
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			log("onDestroyActionMode");
			clearSelections();
			adapter.setMultipleSelect(false);
			Util.changeStatusBarColorActionMode(context, ((FileExplorerActivityLollipop) context).getWindow(), handler, 0);
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			log("onPrepareActionMode");
			List<MegaNode> selected = adapter.getSelectedNodes();

			if (selected.size() != 0) {
				MenuItem unselect = menu.findItem(R.id.cab_menu_unselect_all);
				MegaNode nodeS = megaApi.getNodeByHandle(parentHandle);

				if(selected.size() == megaApi.getNumChildFiles(nodeS)){
					menu.findItem(R.id.cab_menu_select_all).setVisible(false);
					unselect.setTitle(getString(R.string.action_unselect_all));
					unselect.setVisible(true);

				}else{
					if(modeCloud==FileExplorerActivityLollipop.SELECT){
						if(selectFile){
							if(((FileExplorerActivityLollipop)context).multiselect){
								MegaNode node = megaApi.getNodeByHandle(parentHandle);
								if(selected.size() == megaApi.getNumChildFiles(node)){
									menu.findItem(R.id.cab_menu_select_all).setVisible(false);
								}else{
									menu.findItem(R.id.cab_menu_select_all).setVisible(true);
								}
							}
						}
					}else{
						menu.findItem(R.id.cab_menu_select_all).setVisible(true);
					}

					unselect.setTitle(getString(R.string.action_unselect_all));
					unselect.setVisible(true);

//					menu.findItem(R.id.cab_menu_select_all).setVisible(true);
//					unselect.setTitle(getString(R.string.action_unselect_all));
//					unselect.setVisible(true);

				}
			}
			else{
				menu.findItem(R.id.cab_menu_select_all).setVisible(true);
				menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);
			}

			return false;
		}
	}


	public static CloudDriveExplorerFragmentLollipop newInstance() {
		log("newInstance");
		CloudDriveExplorerFragmentLollipop fragment = new CloudDriveExplorerFragmentLollipop();
		return fragment;
	}

	@Override
	public void onCreate (Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		log("onCreate");
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		if (megaApi.getRootNode() == null){
			return;
		}
		
		parentHandle = -1;
		dbH = DatabaseHandler.getDbHandler(context);
		lastPositionStack = new Stack<>();
		handler = new Handler();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		handler.removeCallbacksAndMessages(null);
	}

	public void checkScroll () {
		if (recyclerView == null) {
			return;
		}
		if (recyclerView.canScrollVertically(-1)){
			((FileExplorerActivityLollipop) context).changeActionBarElevation(true);
		}
		else {
			((FileExplorerActivityLollipop) context).changeActionBarElevation(false);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		log("onCreateView");
				
		View v = inflater.inflate(R.layout.fragment_fileexplorerlist, container, false);
		Display display = getActivity().getWindowManager().getDefaultDisplay();
		
		metrics = new DisplayMetrics();
		display.getMetrics(metrics);
		
		float density  = getResources().getDisplayMetrics().density;
		
	    float scaleW = Util.getScaleW(metrics, density);
	    float scaleH = Util.getScaleH(metrics, density);

		separator = (View) v.findViewById(R.id.separator);
		
		optionsBar = (LinearLayout) v.findViewById(R.id.options_explorer_layout);
		optionButton = (Button) v.findViewById(R.id.action_text);
		optionButton.setOnClickListener(this);

		cancelButton = (Button) v.findViewById(R.id.cancel_text);
		cancelButton.setOnClickListener(this);
		cancelButton.setText(getString(R.string.general_cancel).toUpperCase(Locale.getDefault()));

		if (((FileExplorerActivityLollipop) context).isList) {
			recyclerView = (RecyclerView) v.findViewById(R.id.file_list_view_browser);
			v.findViewById(R.id.file_grid_view_browser).setVisibility(View.GONE);
			recyclerView.addItemDecoration(new SimpleDividerItemDecoration(context, metrics));
			mLayoutManager = new LinearLayoutManager(context);
			recyclerView.setLayoutManager(mLayoutManager);
		}
		else {
			recyclerView = (NewGridRecyclerView) v.findViewById(R.id.file_grid_view_browser);
			v.findViewById(R.id.file_list_view_browser).setVisibility(View.GONE);
			gridLayoutManager = (CustomizedGridLayoutManager) recyclerView.getLayoutManager();

		}

		recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
				super.onScrolled(recyclerView, dx, dy);
				checkScroll();
			}
		});
		
		contentText = (TextView) v.findViewById(R.id.content_text);
		contentText.setVisibility(View.GONE);

		emptyImageView = (ImageView) v.findViewById(R.id.file_list_empty_image);
		emptyTextView = (LinearLayout) v.findViewById(R.id.file_list_empty_text);
		emptyTextViewFirst = (TextView) v.findViewById(R.id.file_list_empty_text_first);

		modeCloud = ((FileExplorerActivityLollipop)context).getMode();
		selectFile = ((FileExplorerActivityLollipop)context).isSelectFile();

		parentHandle = ((FileExplorerActivityLollipop)context).parentHandleCloud;
		
		if(modeCloud==FileExplorerActivityLollipop.SELECT_CAMERA_FOLDER){
			parentHandle = -1;
		}
		else if (parentHandle == -1) {
			parentHandle = megaApi.getRootNode().getHandle();
		}

		MegaPreferences prefs = Util.getPreferences(context);
		if(prefs.getPreferredSortCloud()!=null){
			order = Integer.parseInt(prefs.getPreferredSortCloud());
		}
		else{
			order = megaApi.ORDER_DEFAULT_ASC;
		}

		MegaNode chosenNode = megaApi.getNodeByHandle(parentHandle);
		if(chosenNode == null) {
			log("chosenNode is NULL");

			if(megaApi.getRootNode()!=null){
				parentHandle = megaApi.getRootNode().getHandle();
				nodes = megaApi.getChildren(megaApi.getRootNode(), order);
			}

		}else if(chosenNode.getType() == MegaNode.TYPE_ROOT) {
			log("chosenNode is ROOT");
			parentHandle = megaApi.getRootNode().getHandle();
			nodes = megaApi.getChildren(chosenNode, order);

		}else {
			log("ChosenNode not null and not ROOT");

			MegaNode parentNode = megaApi.getParentNode(chosenNode);
			if(parentNode!=null){
				log("ParentNode NOT NULL");
				MegaNode grandParentNode = megaApi.getParentNode(parentNode);
				while(grandParentNode!=null){
					parentNode=grandParentNode;
					grandParentNode = megaApi.getParentNode(parentNode);
				}
				if(parentNode.getType() == MegaNode.TYPE_ROOT){
					nodes = megaApi.getChildren(chosenNode, order);
					log("chosenNode is: "+chosenNode.getName());
				}
				else{
					log("Parent node exists but is not Cloud!");
					parentHandle = megaApi.getRootNode().getHandle();
					nodes = megaApi.getChildren(megaApi.getRootNode(), order);
				}

			}
			else{
				log("parentNode is NULL");
				parentHandle = megaApi.getRootNode().getHandle();
				nodes = megaApi.getChildren(megaApi.getRootNode(), order);
			}

		}
		
		((FileExplorerActivityLollipop)context).setParentHandle(parentHandle);


		if (modeCloud == FileExplorerActivityLollipop.MOVE) {
			optionButton.setText(getString(R.string.context_move).toUpperCase(Locale.getDefault()));

			MegaNode parent = ((FileExplorerActivityLollipop)context).parentMoveCopy();
			if(parent != null){
				if(parent.getHandle() == chosenNode.getHandle()) {
					activateButton(false);
				}else{
					activateButton(true);
				}
			}else{
				activateButton(true);
			}

			nodeHandleMoveCopy = ((FileExplorerActivityLollipop)context).getNodeHandleMoveCopy();
			setDisableNodes(nodeHandleMoveCopy);

		}
		else if (modeCloud == FileExplorerActivityLollipop.COPY){
			optionButton.setText(getString(R.string.context_copy).toUpperCase(Locale.getDefault()));

			MegaNode parent = ((FileExplorerActivityLollipop)context).parentMoveCopy();
			if(parent != null){
				if(parent.getHandle() == chosenNode.getHandle()) {
					activateButton(false);
				}else{
					activateButton(true);
				}
			}else{
				activateButton(true);
			}

		}
		else if (modeCloud == FileExplorerActivityLollipop.UPLOAD){
			optionButton.setText(getString(R.string.context_upload).toUpperCase(Locale.getDefault()));
		}
		else if (modeCloud == FileExplorerActivityLollipop.IMPORT){
			optionButton.setText(getString(R.string.add_to_cloud_import).toUpperCase(Locale.getDefault()));
		}
		else if (modeCloud == FileExplorerActivityLollipop.SELECT || modeCloud == FileExplorerActivityLollipop.SELECT_CAMERA_FOLDER){
			optionButton.setText(getString(R.string.general_select).toUpperCase(Locale.getDefault()));
		}
		else {
			optionButton.setText(getString(R.string.general_select).toUpperCase(Locale.getDefault()));
		}

		if(modeCloud==FileExplorerActivityLollipop.SELECT){
			if(selectFile)
			{
				if(((FileExplorerActivityLollipop)context).multiselect){
					separator.setVisibility(View.VISIBLE);
					optionsBar.setVisibility(View.VISIBLE);
					optionButton.setText(getString(R.string.context_send));
					activateButton(false);
				}
				else{
					separator.setVisibility(View.GONE);
					optionsBar.setVisibility(View.GONE);
				}
			}
			else{
				if(parentHandle==-1||parentHandle==megaApi.getRootNode().getHandle()){
					separator.setVisibility(View.GONE);
					optionsBar.setVisibility(View.GONE);
				}
				else{
					separator.setVisibility(View.VISIBLE);
					optionsBar.setVisibility(View.VISIBLE);
				}
			}
		}

		addSectionTitle(nodes, ((FileExplorerActivityLollipop) context).getItemType());
		if (adapter == null){
			if(selectFile){
				log("Mode SELECT FILE ON");
			}
			adapter = new MegaExplorerLollipopAdapter(context, this, nodes, parentHandle, recyclerView, selectFile);
		}
		else{
			adapter.setParentHandle(parentHandle);
			adapter.setSelectFile(selectFile);
		}
		adapter.setNodes(nodes);

		adapter.setPositionClicked(-1);		
		
		recyclerView.setAdapter(adapter);

		//If folder has no files
		showEmptyScreen();

		return v;
	}

	private void showEmptyScreen() {
		if (adapter == null) {
			return;
		}
		if (adapter.getItemCount() == 0){
			recyclerView.setVisibility(View.GONE);
			emptyImageView.setVisibility(View.VISIBLE);
			emptyTextView.setVisibility(View.VISIBLE);
			if (megaApi.getRootNode().getHandle()==parentHandle) {

				if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
					emptyImageView.setImageResource(R.drawable.cloud_empty_landscape);
				}else{
					emptyImageView.setImageResource(R.drawable.ic_empty_cloud_drive);
				}
				String textToShow = String.format(context.getString(R.string.context_empty_cloud_drive));
				try{
					textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
					textToShow = textToShow.replace("[/A]", "</font>");
					textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
					textToShow = textToShow.replace("[/B]", "</font>");
				}
				catch (Exception e){}
				Spanned result = null;
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
					result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
				} else {
					result = Html.fromHtml(textToShow);
				}
				emptyTextViewFirst.setText(result);

			} else {
//				emptyImageView.setImageResource(R.drawable.ic_empty_folder);
//				emptyTextViewFirst.setText(R.string.file_browser_empty_folder);
				if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
					emptyImageView.setImageResource(R.drawable.ic_zero_landscape_empty_folder);
				}else{
					emptyImageView.setImageResource(R.drawable.ic_zero_portrait_empty_folder);
				}
				String textToShow = String.format(context.getString(R.string.file_browser_empty_folder_new));
				try{
					textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
					textToShow = textToShow.replace("[/A]", "</font>");
					textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
					textToShow = textToShow.replace("[/B]", "</font>");
				}
				catch (Exception e){}
				Spanned result = null;
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
					result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
				} else {
					result = Html.fromHtml(textToShow);
				}
				emptyTextViewFirst.setText(result);
			}

		}
		else{
			recyclerView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
		}
	}

	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }
	
	@Override
	public void onClick(View v) {
		log("onClick");

		switch(v.getId()){
			case R.id.action_text:{
				dbH.setLastCloudFolder(Long.toString(parentHandle));
				if(((FileExplorerActivityLollipop)context).multiselect){
					log("Send several files to chat");
					if(adapter.getSelectedItemCount()>0){
						long handles[] = adapter.getSelectedHandles();
						((FileExplorerActivityLollipop) context).buttonClick(handles);
					}
					else{
						((FileExplorerActivityLollipop) context).showSnackbar(getString(R.string.no_files_selected_warning));
					}

				}
				else{
					((FileExplorerActivityLollipop) context).buttonClick(parentHandle);
				}
				break;
			}
			case R.id.cancel_text:{
				((FileExplorerActivityLollipop) context).finishActivity();
			}
			break;
		}
	}

	public void navigateToFolder(long handle) {
		log("navigateToFolder");

		int lastFirstVisiblePosition = 0;
		if (((FileExplorerActivityLollipop) context).isList()) {
			lastFirstVisiblePosition = mLayoutManager.findFirstCompletelyVisibleItemPosition();
		}
		else {
			lastFirstVisiblePosition = gridLayoutManager.findFirstCompletelyVisibleItemPosition();
		}

		log("Push to stack "+lastFirstVisiblePosition+" position");
		lastPositionStack.push(lastFirstVisiblePosition);

		parentHandle = handle;

		adapter.setParentHandle(parentHandle);
		nodes.clear();
		adapter.setNodes(nodes);
		recyclerView.scrollToPosition(0);

		((FileExplorerActivityLollipop) context).changeTitle();

		//If folder has no files
		if (adapter.getItemCount() == 0){
			recyclerView.setVisibility(View.GONE);
			emptyImageView.setVisibility(View.VISIBLE);
			emptyTextView.setVisibility(View.VISIBLE);
//			emptyImageView.setImageResource(R.drawable.ic_empty_folder);
//			emptyTextViewFirst.setText(R.string.file_browser_empty_folder);
			if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
				emptyImageView.setImageResource(R.drawable.ic_zero_landscape_empty_folder);
			}else{
				emptyImageView.setImageResource(R.drawable.ic_zero_portrait_empty_folder);
			}
			String textToShow = String.format(context.getString(R.string.file_browser_empty_folder_new));
			try{
				textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
				textToShow = textToShow.replace("[/A]", "</font>");
				textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
				textToShow = textToShow.replace("[/B]", "</font>");
			}
			catch (Exception e){}
			Spanned result = null;
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
				result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
			} else {
				result = Html.fromHtml(textToShow);
			}
			emptyTextViewFirst.setText(result);
		}
		else{
			recyclerView.setVisibility(View.VISIBLE);
			emptyTextView.setVisibility(View.GONE);
		}

		if((modeCloud == FileExplorerActivityLollipop.MOVE) || (modeCloud == FileExplorerActivityLollipop.COPY)){
			activateButton(true);
		}
	}

    public void itemClick(View view, int position) {
		log("itemClick");

		ArrayList<MegaNode> clickNodes;

		if (((FileExplorerActivityLollipop) context).isSearchExpanded() && searchNodes != null) {
			clickNodes = searchNodes;
			((FileExplorerActivityLollipop) context).collapseSearchView();
		}
		else {
			clickNodes = nodes;
		}

		if (clickNodes.get(position).isFolder()){
			if(selectFile && ((FileExplorerActivityLollipop)context).multiselect && adapter.isMultipleSelect()){
					hideMultipleSelect();
			}

			MegaNode n = clickNodes.get(position);

			int lastFirstVisiblePosition = 0;
			if (((FileExplorerActivityLollipop)context).isList) {
				lastFirstVisiblePosition = mLayoutManager.findFirstCompletelyVisibleItemPosition();
			}
			else {
				lastFirstVisiblePosition = gridLayoutManager.findFirstCompletelyVisibleItemPosition();
			}

			log("Push to stack "+lastFirstVisiblePosition+" position");
			lastPositionStack.push(lastFirstVisiblePosition);
			
//			String path=n.getName();
//			String[] temp;
//			temp = path.split("/");
//			name = temp[temp.length-1];

			if(n.getType() != MegaNode.TYPE_ROOT)
			{
				if(modeCloud==FileExplorerActivityLollipop.SELECT){
					if(!selectFile)
					{
						separator.setVisibility(View.VISIBLE);
						optionsBar.setVisibility(View.VISIBLE);

					}
					else
					{
						if(((FileExplorerActivityLollipop)context).multiselect){
							separator.setVisibility(View.VISIBLE);
							optionsBar.setVisibility(View.VISIBLE);
							optionButton.setText(getString(R.string.context_send));
						}
						else{
							separator.setVisibility(View.GONE);
							optionsBar.setVisibility(View.GONE);
						}

					}
				}
			}
			else
			{
				if(modeCloud==FileExplorerActivityLollipop.SELECT){
					separator.setVisibility(View.GONE);
					optionsBar.setVisibility(View.GONE);
				}
			}
			
			parentHandle = clickNodes.get(position).getHandle();

			((FileExplorerActivityLollipop)context).setParentHandle(parentHandle);

			adapter.setParentHandle(parentHandle);
			nodes = megaApi.getChildren(clickNodes.get(position), order);
			addSectionTitle(nodes, ((FileExplorerActivityLollipop) context).getItemType());
			adapter.setNodes(nodes);
			recyclerView.scrollToPosition(0);

			((FileExplorerActivityLollipop) context).changeTitle();
			
			//If folder has no files
			if (adapter.getItemCount() == 0){
				recyclerView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
				if (megaApi.getRootNode().getHandle()==n.getHandle()) {
					if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
						emptyImageView.setImageResource(R.drawable.cloud_empty_landscape);
					}else{
						emptyImageView.setImageResource(R.drawable.ic_empty_cloud_drive);
					}
					String textToShow = String.format(context.getString(R.string.context_empty_cloud_drive));
					try{
						textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
						textToShow = textToShow.replace("[/A]", "</font>");
						textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
						textToShow = textToShow.replace("[/B]", "</font>");
					}
					catch (Exception e){}
					Spanned result = null;
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
						result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
					} else {
						result = Html.fromHtml(textToShow);
					}
					emptyTextViewFirst.setText(result);


				} else {
//					emptyImageView.setImageResource(R.drawable.ic_empty_folder);
//					emptyTextViewFirst.setText(R.string.file_browser_empty_folder);
					if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
						emptyImageView.setImageResource(R.drawable.ic_zero_landscape_empty_folder);
					}else{
						emptyImageView.setImageResource(R.drawable.ic_zero_portrait_empty_folder);
					}
					String textToShow = String.format(context.getString(R.string.file_browser_empty_folder_new));
					try{
						textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
						textToShow = textToShow.replace("[/A]", "</font>");
						textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
						textToShow = textToShow.replace("[/B]", "</font>");
					}
					catch (Exception e){}
					Spanned result = null;
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
						result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
					} else {
						result = Html.fromHtml(textToShow);
					}
					emptyTextViewFirst.setText(result);
				}
				if((modeCloud == FileExplorerActivityLollipop.MOVE) || (modeCloud == FileExplorerActivityLollipop.COPY)){
					activateButton(true);
				}

			}
			else{
				recyclerView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);

				if((modeCloud == FileExplorerActivityLollipop.MOVE) || (modeCloud == FileExplorerActivityLollipop.COPY)){

					MegaNode parent = ((FileExplorerActivityLollipop)context).parentMoveCopy();
					if(parent != null){
						if(parent.getHandle() == parentHandle) {
							activateButton(false);
						}else{
							activateButton(true);
						}
					}else{
						activateButton(true);
					}
				}
			}

		}
		else {
			//Is file
			if(selectFile)
			{
				if(((FileExplorerActivityLollipop)context).multiselect){
					log("select file and allow multiselection");

					if (adapter.getSelectedItemCount() == 0) {
						log("activate the actionMode");
						activateActionMode();
						adapter.toggleSelection(position);
						updateActionModeTitle();
					}
					else {
						log("add to selectedNodes");
						adapter.toggleSelection(position);

						List<MegaNode> selectedNodes = adapter.getSelectedNodes();
						if (selectedNodes.size() > 0){
							updateActionModeTitle();
						}
					}

				}
				else{
					//Send file
					MegaNode n = clickNodes.get(position);
					log("Selected node to send: "+n.getName());
					if(clickNodes.get(position).isFile()){
						MegaNode nFile = clickNodes.get(position);
						((FileExplorerActivityLollipop) context).buttonClick(nFile.getHandle());
					}
				}

			}
			else{
				log("Not select file enabled!");
			}
		}



	}	

	public int onBackPressed(){
		log("onBackPressed");
		if(selectFile) {
			if(((FileExplorerActivityLollipop)context).multiselect){
				if(adapter.isMultipleSelect()){
					hideMultipleSelect();
				}
			}
		}
		
		parentHandle = adapter.getParentHandle();

		MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle));

		if (parentNode != null){

			if(parentNode.getType()==MegaNode.TYPE_ROOT){
				parentHandle=-1;

				if(modeCloud==FileExplorerActivityLollipop.SELECT){
					if(!selectFile)
					{
						separator.setVisibility(View.GONE);
						optionsBar.setVisibility(View.GONE);
					}
					else
					{
						if(((FileExplorerActivityLollipop)context).multiselect){
							separator.setVisibility(View.VISIBLE);
							optionsBar.setVisibility(View.VISIBLE);
							optionButton.setText(getString(R.string.context_send));
						}
						else{
							separator.setVisibility(View.GONE);
							optionsBar.setVisibility(View.GONE);
						}
					}
				}

				((FileExplorerActivityLollipop) context).changeTitle();
			}
			else{

				if(modeCloud==FileExplorerActivityLollipop.SELECT){
					if(!selectFile)
					{
						separator.setVisibility(View.VISIBLE);
						optionsBar.setVisibility(View.VISIBLE);
					}
					else
					{
						if(((FileExplorerActivityLollipop)context).multiselect){
							separator.setVisibility(View.VISIBLE);
							optionsBar.setVisibility(View.VISIBLE);
							optionButton.setText(getString(R.string.context_send));
						}
						else{
							separator.setVisibility(View.GONE);
							optionsBar.setVisibility(View.GONE);
						}

					}
				}
				parentHandle = parentNode.getHandle();
				((FileExplorerActivityLollipop) context).changeTitle();
			}

			if((modeCloud == FileExplorerActivityLollipop.MOVE) || (modeCloud == FileExplorerActivityLollipop.COPY)){
				MegaNode parent = ((FileExplorerActivityLollipop)context).parentMoveCopy();
				if(parent != null){
					if(parent.getHandle() == parentNode.getHandle()) {
						activateButton(false);
					}else{
						activateButton(true);
					}
				}else{
					activateButton(true);

				}
			}

			recyclerView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);

			nodes = megaApi.getChildren(parentNode, order);
			addSectionTitle(nodes, ((FileExplorerActivityLollipop) context).getItemType());
			adapter.setNodes(nodes);
			int lastVisiblePosition = 0;
			if(!lastPositionStack.empty()){
				lastVisiblePosition = lastPositionStack.pop();
				log("Pop of the stack "+lastVisiblePosition+" position");
			}
			log("Scroll to "+lastVisiblePosition+" position");

			if(lastVisiblePosition>=0){
				if (((FileExplorerActivityLollipop) context).isList()) {
					mLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
				}
				else {
					gridLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
				}
			}
			adapter.setParentHandle(parentHandle);
			((FileExplorerActivityLollipop)context).setParentHandle(parentHandle);


			return 2;
		}
		else{
			return 0;
		}
	}
	
	/*
	 * Disable nodes from the list
	 */
	public void setDisableNodes(ArrayList<Long> disabledNodes) {
		log("setDisableNodes");
		if (adapter == null){
			log("Adapter is NULL");
			adapter = new MegaExplorerLollipopAdapter(context, this, nodes, parentHandle, recyclerView, selectFile);
		}
		adapter.setDisableNodes(disabledNodes);
		adapter.setSelectFile(selectFile);
	}

	private static void log(String log) {
		Util.log("CloudDriveExplorerFragmentLollipop", log);
	}
	
	public long getParentHandle(){
		log("getParentHandle");
		return adapter.getParentHandle();
	}
	
	public void setParentHandle(long parentHandle){
		log("setParentHandle");
		this.parentHandle = parentHandle;
		if (adapter != null){
			adapter.setParentHandle(parentHandle);
		}
		((FileExplorerActivityLollipop)context).setParentHandle(parentHandle);
	}
	
	public void setNodes(ArrayList<MegaNode> nodes){
		log("setNodes");
		this.nodes = nodes;
		if (adapter != null){
			addSectionTitle(nodes, ((FileExplorerActivityLollipop) context).getItemType());
			adapter.setNodes(nodes);
			if (adapter.getItemCount() == 0){
				recyclerView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
				if (megaApi.getRootNode().getHandle()==parentHandle) {
					if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
						emptyImageView.setImageResource(R.drawable.cloud_empty_landscape);
					}else{
						emptyImageView.setImageResource(R.drawable.ic_empty_cloud_drive);
					}
					String textToShow = String.format(context.getString(R.string.context_empty_cloud_drive));
					try{
						textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
						textToShow = textToShow.replace("[/A]", "</font>");
						textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
						textToShow = textToShow.replace("[/B]", "</font>");
					}
					catch (Exception e){}
					Spanned result = null;
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
						result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
					} else {
						result = Html.fromHtml(textToShow);
					}
					emptyTextViewFirst.setText(result);
				} else {
//					emptyImageView.setImageResource(R.drawable.ic_empty_folder);
//					emptyTextViewFirst.setText(R.string.file_browser_empty_folder);
					if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
						emptyImageView.setImageResource(R.drawable.ic_zero_landscape_empty_folder);
					}else{
						emptyImageView.setImageResource(R.drawable.ic_zero_portrait_empty_folder);
					}
					String textToShow = String.format(context.getString(R.string.file_browser_empty_folder_new));
					try{
						textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
						textToShow = textToShow.replace("[/A]", "</font>");
						textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
						textToShow = textToShow.replace("[/B]", "</font>");
					}
					catch (Exception e){}
					Spanned result = null;
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
						result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
					} else {
						result = Html.fromHtml(textToShow);
					}
					emptyTextViewFirst.setText(result);
				}
			}
			else{
				recyclerView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}
		}
	}

	public void selectAll(){
		log("selectAll");
		if (adapter != null){
			adapter.selectAll();

			updateActionModeTitle();
		}
	}

	public boolean isFolder(int position){
		MegaNode node = nodes.get(position);
		if(node.isFolder()){
			return true;
		}
		else{
			return false;
		}
	}

	/*
	 * Clear all selected items
	 */
	private void clearSelections() {
		if(adapter.isMultipleSelect()){
			adapter.clearSelections();
		}
	}

	private void updateActionModeTitle() {
		log("updateActionModeTitle");

		List<MegaNode> documents = adapter.getSelectedNodes();
		int files = 0;
		int folders = 0;
		for (MegaNode document : documents) {
			if (document.isFile()) {
				files++;
			} else if (document.isFolder()) {
				folders++;
			}
		}


		Resources res = getActivity().getResources();

		String title;
		int sum=files+folders;

		if (files == 0 && folders == 0) {
			title = Integer.toString(sum);
		} else if (files == 0) {
			title = Integer.toString(folders);
		} else if (folders == 0) {
			title = Integer.toString(files);
		} else {
			title = Integer.toString(sum);
		}
		actionMode.setTitle(title);


		try {
			actionMode.invalidate();
		} catch (NullPointerException e) {
			e.printStackTrace();
			log("oninvalidate error");
		}
	}

	/*
	 * Disable selection
	 */
	public void hideMultipleSelect() {
		log("hideMultipleSelect");
		adapter.setMultipleSelect(false);
		adapter.clearSelections();
		if (actionMode != null) {
			actionMode.finish();
		}

		if(isMultiselect()){
			activateButton(false);
		}

	}

	public RecyclerView getRecyclerView(){
		return recyclerView;
	}

	public void activateButton(boolean show){
		optionButton.setEnabled(show);
		if(show){
			optionButton.setTextColor(ContextCompat.getColor(context, R.color.accentColor));
		}else{
			optionButton.setTextColor(ContextCompat.getColor(context, R.color.invite_button_deactivated));
		}
	}

	public void orderNodes (int order) {
		this.order = order;
		if (parentHandle == -1) {
			nodes = megaApi.getChildren(megaApi.getRootNode(), order);
		}
		else {
			nodes = megaApi.getChildren(megaApi.getNodeByHandle(parentHandle), order);
		}

		addSectionTitle(nodes, ((FileExplorerActivityLollipop) context).getItemType());
		adapter.setNodes(nodes);
	}

	boolean isMultiselect() {
		if (modeCloud==FileExplorerActivityLollipop.SELECT && selectFile && ((FileExplorerActivityLollipop) context).multiselect) {
			return true;
		}
		return false;
	}

	public void search (String s) {
		if (megaApi == null || s == null) {
			return;
		}
		if (getParentHandle() == -1) {
			setParentHandle(megaApi.getRootNode().getHandle());
		}
		MegaNode parent = megaApi.getNodeByHandle(getParentHandle());
		if (parent == null) {
			return;
		}
		searchNodes = megaApi.search(parent, s, false, order);
		if (searchNodes != null && adapter != null) {
			addSectionTitle(searchNodes, ((FileExplorerActivityLollipop) context).getItemType());
			adapter.setNodes(searchNodes);
		}
		showEmptyScreen();
	}

	public void closeSearch() {
		searchNodes = null;
		if (adapter == null) {
			return;
		}
		addSectionTitle(nodes, ((FileExplorerActivityLollipop) context).getItemType());
		adapter.setNodes(nodes);
		showEmptyScreen();
	}

	private void addSectionTitle(List<MegaNode> nodes,int type) {
		Map<Integer, String> sections = new HashMap<>();
		int placeholderCount;
		int folderCount = 0;
		int fileCount = 0;
		for (MegaNode node : nodes) {
			if(node == null) {
				continue;
			}
			if (node.isFolder()) {
				folderCount++;
			}
			if (node.isFile()) {
				fileCount++;
			}
		}

		if (type == MegaNodeAdapter.ITEM_VIEW_TYPE_GRID) {
			int spanCount = 2;
			if (recyclerView instanceof NewGridRecyclerView) {
				spanCount = ((NewGridRecyclerView)recyclerView).getSpanCount();
			}
			if(folderCount > 0) {
				for (int i = 0;i < spanCount;i++) {
					sections.put(i,getString(R.string.general_folders));
				}
			}

			if(fileCount > 0 ) {
				placeholderCount = (folderCount % spanCount) == 0 ? 0 : spanCount - (folderCount % spanCount);
				if (placeholderCount == 0) {
					for (int i = 0;i < spanCount;i++) {
						sections.put(folderCount + i,getString(R.string.general_files));
					}
				} else {
					for (int i = 0;i < spanCount;i++) {
						sections.put(folderCount + placeholderCount + i,getString(R.string.general_files));
					}
				}
			}
		} else {
			sections.put(0,getString(R.string.general_folders));
			sections.put(folderCount,getString(R.string.general_files));
		}

		if (headerItemDecoration == null) {
			headerItemDecoration = new NewHeaderItemDecoration(context);
			recyclerView.addItemDecoration(headerItemDecoration);
		}
		headerItemDecoration.setType(type);
		headerItemDecoration.setKeys(sections);
	}
}
