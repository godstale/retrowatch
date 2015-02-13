package com.hardcopy.retrowatch;

import com.hardcopy.retrowatch.contents.objects.ContentObject;
import com.hardcopy.retrowatch.contents.objects.FilterObject;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

 public class MenuDialog extends Dialog {

	// Global
	public static final String tag = "MessageListDialog";
	
	private static final int DIALOG_MODE_MESSAGE = 1;
	private static final int DIALOG_MODE_FILTER = 2;
	
	private String mDialogTitle;
	
	// Context, system
	private Context mContext;
	private IDialogListener mDialogListener;
	private OnClickListener mClickListener;
	
	// Layout
	private Button mBtnDisablePackage;
	private Button mBtnEdit;
	private Button mBtnClose;
	
	
	// Params
	private ContentObject mContentObject = null;
	private FilterObject mFilterObject = null;
	private int mMode = DIALOG_MODE_MESSAGE;
	
	// Constructor
    public MenuDialog(Context context) {
        super(context);
        mContext = context;
    }
    public MenuDialog(Context context, int theme) {
        super(context, theme);
        mContext = context;
    }
	
	/*****************************************************
	 *		Overrided methods
	 ******************************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
         
    	//----- Set title
    	if(mDialogTitle != null) {
    		setTitle(mDialogTitle);
    	} else {
    		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
    	}
        
        WindowManager.LayoutParams lpWindow = new WindowManager.LayoutParams();    
        lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        lpWindow.dimAmount = 0.8f;
        getWindow().setAttributes(lpWindow);

        setContentView(R.layout.dialog_filter_list);
        mClickListener = new OnClickListener(this);
        
        mBtnDisablePackage = (Button) findViewById(R.id.btn_disable_package);
        mBtnDisablePackage.setOnClickListener(mClickListener);
        mBtnEdit = (Button) findViewById(R.id.btn_edit);
        mBtnEdit.setOnClickListener(mClickListener);
        
        setContent();
    }
    
    @Override
    protected  void onStop() {
    	super.onStop();
    }

	
	/*****************************************************
	 *		Public methods
	 ******************************************************/
    public void setDialogParams(IDialogListener listener, String title, ContentObject co, FilterObject fo) {
    	mDialogListener = listener;
    	mDialogTitle = title;
    	if(co != null) {
    		mMode = DIALOG_MODE_MESSAGE;
    	}
    	mContentObject = co;
    	if(fo != null) {
    		mMode = DIALOG_MODE_FILTER;
    	}
    	mFilterObject = fo;
    }
    
	/*****************************************************
	 *		Private methods
	 ******************************************************/
    private void setContent() {

    }	// End of setContent()
	
	/*****************************************************
	 *		Sub classes
	 ******************************************************/
	private class OnClickListener implements View.OnClickListener 
	{
		MenuDialog mDialogContext;
		
		public OnClickListener(MenuDialog context) {
			mDialogContext = context;
		}
		
		@Override
		public void onClick(View v) 
		{
			switch(v.getId())
			{
				case R.id.btn_disable_package:
					mDialogContext.dismiss();
					if(mDialogListener != null)
						mDialogListener.OnDialogCallback(IDialogListener.CALLBACK_DISABLE_PACKAGE, 0, 0, 
								null, null, mMode == DIALOG_MODE_MESSAGE ? mContentObject : mFilterObject);
					break;
					
				case R.id.btn_edit:
					mDialogContext.dismiss();
					if(mDialogListener != null)
						mDialogListener.OnDialogCallback(IDialogListener.CALLBACK_CLOSE, 0, 0, 
								null, null, mMode == DIALOG_MODE_MESSAGE ? mContentObject : mFilterObject);
					break;
			}
		}
	}	// End of class OnClickListener
}
