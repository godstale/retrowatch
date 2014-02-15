package com.hardcopy.retrowatchle;

import com.hardcopy.retrowatchle.contents.objects.ContentObject;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

 public class MessageListDialog extends Dialog {

	// Global
	public static final String tag = "MessageListDialog";
	
	private String mDialogTitle;
	
	// Context, system
	private Context mContext;
	private IDialogListener mDialogListener;
	private OnClickListener mClickListener;
	
	// Layout
	private Button mBtnEnableMsg;
	private Button mBtnEnablePackage;
	private Button mBtnClose;
	
	private TextView mTextEnabled;
	
	// Params
	private ContentObject mContentObject;
	
	// Constructor
    public MessageListDialog(Context context) {
        super(context);
        mContext = context;
    }
    public MessageListDialog(Context context, int theme) {
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

        setContentView(R.layout.dialog__message_list);
        mClickListener = new OnClickListener(this);
        
        mBtnEnableMsg = (Button) findViewById(R.id.btn_enable_message);
        mBtnEnableMsg.setOnClickListener(mClickListener);
        mBtnEnablePackage = (Button) findViewById(R.id.btn_enable_package);
        mBtnEnablePackage.setOnClickListener(mClickListener);
        mBtnClose = (Button) findViewById(R.id.btn_close);
        mBtnClose.setOnClickListener(mClickListener);
        
        mTextEnabled = (TextView) findViewById(R.id.text_enabled);
        
        setContent();
    }
    
    @Override
    protected  void onStop() {
    	super.onStop();
    }

	
	/*****************************************************
	 *		Public methods
	 ******************************************************/
    public void setDialogParams(IDialogListener listener, String title, ContentObject co) {
    	mDialogListener = listener;
    	mDialogTitle = title;
    	mContentObject = co;
    }
    
	/*****************************************************
	 *		Private methods
	 ******************************************************/
    private void setContent() {
    	if(mContentObject.mIsEnabled) {
    		mBtnEnableMsg.setVisibility(View.GONE);
    		mBtnEnablePackage.setVisibility(View.GONE);
    		mTextEnabled.setVisibility(View.VISIBLE);
    		mBtnClose.setVisibility(View.VISIBLE);
    	}
    }	// End of setContent()
	
	/*****************************************************
	 *		Sub classes
	 ******************************************************/
	private class OnClickListener implements View.OnClickListener 
	{
		MessageListDialog mDialogContext;
		
		public OnClickListener(MessageListDialog context) {
			mDialogContext = context;
		}
		
		@Override
		public void onClick(View v) 
		{
			switch(v.getId())
			{
				case R.id.btn_enable_message:
					mDialogContext.dismiss();
					if(mDialogListener != null)
						mDialogListener.OnDialogCallback(IDialogListener.CALLBACK_ENABLE_MESSAGE, 0, 0, null, null, mContentObject);
					break;
					
				case R.id.btn_enable_package:
					mDialogContext.dismiss();
					if(mDialogListener != null)
						mDialogListener.OnDialogCallback(IDialogListener.CALLBACK_ENABLE_PACKAGE, 0, 0, null, null, mContentObject);
					break;
					
				case R.id.btn_close:
					mDialogContext.dismiss();
					if(mDialogListener != null)
						mDialogListener.OnDialogCallback(IDialogListener.CALLBACK_CLOSE, 0, 0, null, null, mContentObject);
					break;
			}
		}
	}	// End of class OnClickListener
}
