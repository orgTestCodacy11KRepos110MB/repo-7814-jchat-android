package io.jchat.android.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import cn.jpush.im.android.api.callback.DownloadAvatarCallback;
import cn.jpush.im.android.api.model.UserInfo;
import io.jchat.android.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cn.jpush.im.android.api.JMessageClient;

import io.jchat.android.tools.BitmapLoader;
import io.jchat.android.tools.NativeImageLoader;
import io.jchat.android.view.RoundImageView;

public class GroupMemberGridAdapter extends BaseAdapter {

    private LayoutInflater mInflater;
    //群成员列表
    private List<UserInfo> mMemberList = new ArrayList<UserInfo>();
    //空白项列表
    private ArrayList<String> mBlankList = new ArrayList<String>();
    private boolean mIsCreator = false;
    private boolean mIsShowDelete;
    //群成员个数
    private int mCurrentNum;
    //记录空白项的数组
    private int[] mRestArray = new int[]{2, 1, 0, 3};
    //用群成员项数余4得到，作为下标查找mRestArray，得到空白项
    private int mRestNum;
    private int mDefaultSize;

    public GroupMemberGridAdapter(Context context, List<UserInfo> memberList,
                                  boolean isCreator, boolean isGroup) {
        this.mMemberList = memberList;
        this.mIsCreator = isCreator;
        mInflater = LayoutInflater.from(context);
        mIsShowDelete = false;
        initBlankItem();
        DisplayMetrics dm = new DisplayMetrics();
        ((Activity)context).getWindowManager().getDefaultDisplay().getMetrics(dm);
        mDefaultSize = (int )(50 * dm.density);
    }

    public void initBlankItem() {
        mCurrentNum = mMemberList.size();
        mRestNum = mRestArray[mCurrentNum % 4];
        //补全空白项
        for (int i = 0; i < mRestNum; i++) {
            mBlankList.add("");
        }
    }

    public void refreshGroupMember(List<UserInfo> memberList) {
        mMemberList.clear();
        mMemberList = memberList;
        initBlankItem();
        notifyDataSetChanged();
    }

    public void addMemberToList(UserInfo userInfo) {
        if (!mMemberList.contains(userInfo)) {
            mMemberList.add(userInfo);
        }
        initBlankItem();
        notifyDataSetChanged();
    }

    public void remove(int position) {
        if (position >= mMemberList.size()) {
            return;
        }
        mMemberList.remove(position);
        --mCurrentNum;
        mRestNum = mRestArray[mCurrentNum % 4];
        notifyDataSetChanged();
    }

    public void setIsShowDelete(boolean isShowDelete) {
        this.mIsShowDelete = isShowDelete;
        notifyDataSetChanged();
    }

    public void setIsShowDelete(boolean isShowDelete, int restNum) {
        this.mIsShowDelete = isShowDelete;
        mRestNum = restNum;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        //如果是普通成员，并且只有3个群成员，特殊处理
        if(mCurrentNum == 3 && !mIsCreator)
            return 4;
        else return mCurrentNum + mRestNum + 2;
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ItemViewTag viewTag;
        Bitmap bitmap;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.group_grid_view_item, null);
            viewTag = new ItemViewTag((RoundImageView) convertView.findViewById(R.id.grid_avatar),
                    (TextView) convertView.findViewById(R.id.grid_name),
                    (ImageView) convertView.findViewById(R.id.grid_delete_icon));
            convertView.setTag(viewTag);
        } else {
            viewTag = (ItemViewTag) convertView.getTag();
        }
        //是Delete状态
        if (mIsShowDelete) {
            if (position < mCurrentNum) {
                //群主不能删除自己
                UserInfo userInfo = mMemberList.get(position);
                if (userInfo.getUserName().equals(JMessageClient.getMyInfo().getUserName()))
                    viewTag.deleteIcon.setVisibility(View.GONE);
                else viewTag.deleteIcon.setVisibility(View.VISIBLE);
                viewTag = (ItemViewTag) convertView.getTag();
                bitmap = NativeImageLoader.getInstance().getBitmapFromMemCache(userInfo.getUserName());
                if (bitmap != null)
                    viewTag.icon.setImageBitmap(bitmap);
                else{
                    File file = userInfo.getAvatarFile();
                    if(file != null && file.isFile()){
                        bitmap = BitmapLoader.getBitmapFromFile(file.getAbsolutePath(), mDefaultSize, mDefaultSize);
                        NativeImageLoader.getInstance().updateBitmapFromCache(userInfo.getUserName(), bitmap);
                        viewTag.icon.setImageBitmap(bitmap);
                    }else viewTag.icon.setImageResource(R.drawable.head_icon);
                }
                viewTag.name.setText(userInfo.getNickname());
            } else {
                viewTag.deleteIcon.setVisibility(View.INVISIBLE);
                viewTag.icon.setVisibility(View.INVISIBLE);
                viewTag.name.setVisibility(View.INVISIBLE);
            }
            //非Delete状态
        } else {
            viewTag.deleteIcon.setVisibility(View.INVISIBLE);
            //群成员
            if (position < mCurrentNum) {
                final UserInfo userInfo = mMemberList.get(position);
                viewTag = (ItemViewTag) convertView.getTag();
                bitmap = NativeImageLoader.getInstance().getBitmapFromMemCache(userInfo.getUserName());
                if (bitmap != null)
                    viewTag.icon.setImageBitmap(bitmap);
                //如果内存中不存在头像缓存
                else{
                    File file = userInfo.getAvatarFile();
                    if(file != null && file.isFile()){
                        bitmap = BitmapLoader.getBitmapFromFile(file.getAbsolutePath(),  mDefaultSize,  mDefaultSize);
                        NativeImageLoader.getInstance().updateBitmapFromCache(userInfo.getUserName(), bitmap);
                        viewTag.icon.setImageBitmap(bitmap);
                    }else {
                        viewTag.icon.setImageResource(R.drawable.head_icon);
                        userInfo.getAvatarFileAsync(new DownloadAvatarCallback() {
                            @Override
                            public void gotResult(int status, String desc, File file) {
                                if(status == 0){
                                    Bitmap bitmap = BitmapLoader.getBitmapFromFile(file.getAbsolutePath(),  mDefaultSize,  mDefaultSize);
                                    NativeImageLoader.getInstance().updateBitmapFromCache(userInfo.getUserName(), bitmap);
                                    notifyDataSetChanged();
                                }
                            }
                        });
                    }
                }
                viewTag.name.setText(userInfo.getNickname());
                viewTag.name.setVisibility(View.VISIBLE);
                //设置增加群成员按钮
            } else if (position == mCurrentNum) {
                viewTag = (ItemViewTag) convertView.getTag();
                viewTag.icon.setImageResource(R.drawable.chat_detail_add);
                viewTag.icon.setVisibility(View.VISIBLE);
                viewTag.name.setVisibility(View.INVISIBLE);

                //设置删除群成员按钮
            } else if (position == mCurrentNum + 1) {
                if (mIsCreator && mCurrentNum > 1) {
                    viewTag = (ItemViewTag) convertView.getTag();
                    viewTag.icon.setImageResource(R.drawable.chat_detail_del);
                    viewTag.icon.setVisibility(View.VISIBLE);
                    viewTag.name.setVisibility(View.INVISIBLE);
                } else {
                    viewTag = (ItemViewTag) convertView.getTag();
                    viewTag.icon.setVisibility(View.GONE);
                    viewTag.name.setVisibility(View.GONE);
                }
                //空白项
            } else {
                viewTag = (ItemViewTag) convertView.getTag();
                viewTag.icon.setVisibility(View.INVISIBLE);
                viewTag.name.setVisibility(View.INVISIBLE);
            }
        }


        return convertView;
    }

    public void setCreator(boolean isCreator) {
        mIsCreator = isCreator;
        notifyDataSetChanged();
    }


    class ItemViewTag {

        protected RoundImageView icon;
        protected ImageView deleteIcon;
        protected TextView name;

        public ItemViewTag(RoundImageView icon, TextView name, ImageView deleteIcon) {
            this.icon = icon;
            this.deleteIcon = deleteIcon;
            this.name = name;
        }
    }
}
