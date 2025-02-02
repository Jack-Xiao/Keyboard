package leavesc.hello.keyboard;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.lang.reflect.Field;

/**
 * 作者： chenZY
 * 时间： 2017/8/26 18:12
 * 描述：
 */
public class EmojiKeyboard {

    private Activity activity;

    //文本输入框
    private EditText editText;

    //表情面板
    private View emojiPanelView;

    //内容View,即除了表情布局和输入框布局以外的布局
    //用于固定输入框一行的高度以防止跳闪
    private View contentView;

    private InputMethodManager inputMethodManager;

    private SharedPreferences sharedPreferences;

    private static final String EMOJI_KEYBOARD = "EmojiKeyboard";

    private static final String KEY_SOFT_KEYBOARD_HEIGHT = "SoftKeyboardHeight";

    private static final String CURRENT_KEY_SOFT_KEYBOARD_HEIGHT = "Current_SoftKeyboardHeight";


    private static final int SOFT_KEYBOARD_HEIGHT_DEFAULT = 654;

    private Handler handler;
    private View parentView;

    public EmojiKeyboard(Activity activity, EditText editText, View emojiPanelView, View emojiPanelSwitchView, View contentView) {
        init(activity, editText, emojiPanelView, emojiPanelSwitchView, contentView);
    }

    private void init(Activity activity, EditText editText, View emojiPanelView, View emojiPanelSwitchView, View contentView) {
        this.activity = activity;
        this.editText = editText;
        this.emojiPanelView = emojiPanelView;
        this.contentView = contentView;
        this.editText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(final View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP && EmojiKeyboard.this.emojiPanelView.isShown()) {
                    lockContentViewHeight();
                    hideEmojiPanel(true);
                    unlockContentViewHeight();
                }
                return false;
            }
        });

        parentView = emojiPanelView.getRootView();
        this.contentView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    if (EmojiKeyboard.this.emojiPanelView.isShown()) {
                        hideEmojiPanel(false);
                    } else if (isSoftKeyboardShown()) {
                        hideSoftKeyboard();
                    }
                }
                return false;
            }
        });
        //用于弹出表情面板的View
        emojiPanelSwitchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (EmojiKeyboard.this.emojiPanelView.isShown()) {
                    lockContentViewHeight();
                    hideEmojiPanel(true);
                    unlockContentViewHeight();
                } else {
                    if (isSoftKeyboardShown()) {
                        lockContentViewHeight();
                        showEmojiPanel();
                        unlockContentViewHeight();
                    } else {
                        showEmojiPanel();
                    }
                }
            }
        });
        this.inputMethodManager = (InputMethodManager) this.activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        this.sharedPreferences = this.activity.getSharedPreferences(EMOJI_KEYBOARD, Context.MODE_PRIVATE);
        this.activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        this.handler = new Handler();
        init();
    }

    /**
     * 如果之前没有保存过键盘高度值
     * 则在进入Activity时自动打开键盘，并把高度值保存下来
     */
    private void init() {
        if (!sharedPreferences.contains(KEY_SOFT_KEYBOARD_HEIGHT)) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    showSoftKeyboard(true);
                }
            }, 200);
        }
    }

    /**
     * 当点击返回键时需要先隐藏表情面板
     */
    public boolean interceptBackPress() {
        if (emojiPanelView.isShown()) {
            hideEmojiPanel(false);
            return true;
        }
        return false;
    }

    /**
     * 锁定内容View以防止跳闪
     */
    private void lockContentViewHeight() {
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) contentView.getLayoutParams();
        layoutParams.height = contentView.getHeight();
        layoutParams.weight = 0;
    }

    /**
     * 释放锁定的内容View
     */
    private void unlockContentViewHeight() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                ((LinearLayout.LayoutParams) contentView.getLayoutParams()).weight = 1;
            }
        }, 200);
    }

    /**
     * 获取当前键盘的高度
     */
    private int getSoftKeyboardHeight() {
        int currentHeight = sharedPreferences.getInt(CURRENT_KEY_SOFT_KEYBOARD_HEIGHT, SOFT_KEYBOARD_HEIGHT_DEFAULT);
        return currentHeight;
    }

    /**
     * 获取本地存储的键盘高度值或者是返回默认值
     */
    private int getSoftKeyboardHeightLocalValue() {

        return sharedPreferences.getInt(KEY_SOFT_KEYBOARD_HEIGHT, SOFT_KEYBOARD_HEIGHT_DEFAULT);
    }

    /**
     * 判断是否显示了键盘
     */
    private boolean isSoftKeyboardShown() {
        return getSoftKeyboardHeight() != 0;
    }

    /**
     * 令编辑框获取焦点并显示键盘
     */
    private void showSoftKeyboard(boolean saveSoftKeyboardHeight) {
        editText.requestFocus();
        inputMethodManager.showSoftInput(editText, 0);
        if (saveSoftKeyboardHeight) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    getSoftKeyboardHeight();
                }
            }, 100);
        }
    }

    /**
     * 隐藏键盘
     */
    private void hideSoftKeyboard() {
        inputMethodManager.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

    /**
     * 显示表情面板
     */
    private void showEmojiPanel() {
        int softKeyboardHeight = getSoftKeyboardHeight();
        if (softKeyboardHeight == 0) {
            softKeyboardHeight = getSoftKeyboardHeightLocalValue();
        } else {
            hideSoftKeyboard();
        }
        emojiPanelView.getLayoutParams().height = softKeyboardHeight;
        emojiPanelView.setVisibility(View.VISIBLE);
        if (emojiPanelVisibilityChangeListener != null) {
            emojiPanelVisibilityChangeListener.onShowEmojiPanel();
        }
    }

    /**
     * 隐藏表情面板，同时指定是否随后开启键盘
     */
    private void hideEmojiPanel(boolean showSoftKeyboard) {
        if (emojiPanelView.isShown()) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    emojiPanelView.setVisibility(View.GONE);
                }
            }, 200);

            if (showSoftKeyboard) {
                showSoftKeyboard(false);
            }
            if (emojiPanelVisibilityChangeListener != null) {
                emojiPanelVisibilityChangeListener.onHideEmojiPanel();
            }
        }
    }

    public void storeKeyboardHeight(int realKeyboardHeight) {
        sharedPreferences.edit().putInt(CURRENT_KEY_SOFT_KEYBOARD_HEIGHT, realKeyboardHeight).apply();
        if (realKeyboardHeight != 0) {
            sharedPreferences.edit().putInt(KEY_SOFT_KEYBOARD_HEIGHT, realKeyboardHeight).apply();
        }
    }

    public interface OnEmojiPanelVisibilityChangeListener {

        void onShowEmojiPanel();

        void onHideEmojiPanel();
    }

    private OnEmojiPanelVisibilityChangeListener emojiPanelVisibilityChangeListener;

    public void setEmoticonPanelVisibilityChangeListener(OnEmojiPanelVisibilityChangeListener emojiPanelVisibilityChangeListener) {
        this.emojiPanelVisibilityChangeListener = emojiPanelVisibilityChangeListener;
    }

}
