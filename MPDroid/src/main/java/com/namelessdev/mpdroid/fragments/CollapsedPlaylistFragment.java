/*
 * Copyright (C) 2010-2014 The MPDroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.namelessdev.mpdroid.fragments;

import com.anpmech.mpd.MPDPlaylist;
import com.anpmech.mpd.item.Music;
import com.mobeta.android.dslv.DragSortListView;
import com.namelessdev.mpdroid.MainMenuActivity;
import com.namelessdev.mpdroid.NowPlayingActivity;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.helpers.AlbumInfo;
import com.namelessdev.mpdroid.helpers.QueueControl;
import com.namelessdev.mpdroid.models.AbstractPlaylistMusic;
import com.namelessdev.mpdroid.models.PlaylistAlbum;
import com.namelessdev.mpdroid.models.PlaylistSong;
import com.namelessdev.mpdroid.models.PlaylistStream;
import com.namelessdev.mpdroid.tools.Tools;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupMenu.OnMenuItemClickListener;

import java.util.ArrayList;
import java.util.List;

public class CollapsedPlaylistFragment extends QueueFragment implements OnMenuItemClickListener {


    private final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mApp);

    private final DragSortListView.DropListener onDrop = new DragSortListView.DropListener() {
        @Override
        public void drop(final int from, int to) {
            if (from != to && mFilter == null) {
                boolean moveDown = (to > from);
                if (moveDown) {
                    to++;
                }
                int plFrom = getPlaylistPosition(from);
                int plTo = getPlaylistPosition(to);
                int number = getPlaylistPosition(from + 1) - plFrom;
                if (moveDown) {
                    plTo -= number;
                }
                //Log.d("MPD Coll", "from " + from + " to "+ to + " plFrom " +plFrom + " plTo "+ plTo + " number " + number);
                QueueControl.run(QueueControl.MOVE, plFrom, number, plTo);
            }
        }
    };


    protected boolean collapsedAlbums = false;

    protected List<PlaylistAlbum> playlistAlbums;

    // one Album can be expanded besides currently playing:
    protected int expandedAlbum = -1;

    protected PlaylistAlbum playlistAlbumWithSongId(int songId) {
        if (playlistAlbums == null) {
            return null;
        }
        for (PlaylistAlbum p : playlistAlbums) {
            if (p.hasSongId(songId)) {
                return p;
            }
        }
        return null;
    }


    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);

        // only replace these 2 listeners
        mList.setDropListener(onDrop);
        mList.setMultiChoiceModeListener(new MultiChoiceModeListener() {

            @Override
            public boolean onActionItemClicked(final ActionMode mode, final MenuItem item) {

                final SparseBooleanArray checkedItems = mList.getCheckedItemPositions();
                final int count = mList.getCount();
                final ListAdapter adapter = mList.getAdapter();
                int j = 0;
                boolean result = true;
                final List<Integer> positions = new ArrayList<Integer>();

                switch (item.getItemId()) {
                    case R.id.menu_delete:
                        for (int i = 0; i < count; i++) {
                            if (checkedItems.get(i)) {
                                AbstractPlaylistMusic itemi =
                                        (AbstractPlaylistMusic) adapter.getItem(i);
                                try {
                                    positions.addAll(((PlaylistAlbum) itemi).getSongIds());
                                } catch (ClassCastException e) {
                                    positions.add(itemi.getSongId());
                                }
                            }
                        }
                        result = true;
                    case R.id.menu_crop:
                        for (int i = 0; i < count; i++) {
                            if (!checkedItems.get(i)) {
                                AbstractPlaylistMusic itemi =
                                        (AbstractPlaylistMusic) adapter.getItem(i);
                                try {
                                    positions.addAll(((PlaylistAlbum) itemi).getSongIds());
                                } catch (ClassCastException e) {
                                    positions.add(itemi.getSongId());
                                }
                            }
                        }
                        result = true;
                    default:
                        result = false;
                        break;
                }

                if (j > 0) {
                    QueueControl.run(QueueControl.REMOVE_BY_ID, Tools.toIntArray(positions));
                    mode.finish();
                }

                return result;
            }

            @Override
            public boolean onCreateActionMode(final ActionMode mode, final Menu menu) {
                final MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.mpd_queuemenu, menu);
                return true;
            }

            @Override
            public void onDestroyActionMode(final ActionMode mode) {
                mActionMode = null;
                mController.setSortEnabled(true);
            }

            @Override
            public void onItemCheckedStateChanged(
                    final ActionMode mode, final int position, final long id,
                    final boolean checked) {
                final int selectCount = mList.getCheckedItemCount();
                if (selectCount == 0) {
                    mode.finish();
                }
                if (selectCount == 1) {
                    mode.setTitle(R.string.actionSongSelected);
                } else {
                    mode.setTitle(getString(R.string.actionSongsSelected, selectCount));
                }
            }

            @Override
            public boolean onPrepareActionMode(final ActionMode mode, final Menu menu) {
                mActionMode = mode;
                mController.setSortEnabled(false);
                return false;
            }
        });

        return view;
    }

    @Override
    public void onListItemClick(final ListView l, final View v, final int position, final long id) {
        AbstractPlaylistMusic item =
                (AbstractPlaylistMusic) l.getAdapter().getItem(position);
        try {
            // Try collapsed album
            final PlaylistAlbum plA = (PlaylistAlbum) item;
            expandedAlbum = plA.getSongId();
            update(true, position);
        } catch (ClassCastException cce) {
            expandedAlbum = -1; // none is expanded except current
            super.onListItemClick(l, v, position, id);
            // final int song = ((Music) item).getSongId();
            // QueueControl.run(QueueControl.SKIP_TO_ID, song);
        }
    }

    private static final String TAG = "com.namelessdev.mpdroid.CollapsedPlaylistFragment";

    @Override
    public boolean onMenuItemClick(final MenuItem item) {
        try { // is this item is a collapsed album?
            PlaylistAlbum plAlbum = (PlaylistAlbum) item;
        } catch (ClassCastException cce) {
            // for non-albums just do as normal and finish
            return super.onMenuItemClick(item);
        }
        // handle only cases with special treatment for albums
        boolean done = true;
        switch (item.getItemId()) {
            case R.id.PLCX_playNext:
                Tools.notifyUser("playnext not implemented for albums");
                break;
            case R.id.PLCX_moveFirst:
                Tools.notifyUser("Move to first not implemented for albums");
                break;
            case R.id.PLCX_moveLast:
                Tools.notifyUser("Move to last not implemented for albums");
                break;
            case R.id.PLCX_removeFromPlaylist:
                final PlaylistAlbum plAlbum = playlistAlbumWithSongId(mPopupSongID);
                QueueControl.run(QueueControl.REMOVE_BY_ID,
                        Tools.toIntArray(plAlbum.getSongIds()));
                if (isAdded()) {
                    Tools.notifyUser(R.string.deletedSongFromPlaylist);
                }
                break;
            default:
                done = false; // do like non-album
                break;
        }
        if (!done) {
            return super.onMenuItemClick(item);
        } else {
            return true;
        }
    }

    @Override
    public void scrollToNowPlaying() {
        final int songPos = getListPosition(mApp.getMPD().getStatus().getSongPos());

        if (songPos == -1) {
            Log.d(TAG, "Missing list item.");
        } else {

            if (mActivity instanceof MainMenuActivity) {
                ((NowPlayingActivity) mActivity).showQueue();
            }

            final ListView listView = getListView();
            listView.requestFocusFromTouch();
            listView.setSelection(songPos);
            listView.clearFocus();
        }
    }

    // list item number from playlist position

    protected int getListPosition(final int playlistpos) {
        int sum = 0, pos = 0;
        for (AbstractPlaylistMusic item : mSongList) {
            int s = (int) item.size();
            if (sum + s < playlistpos) {
                sum += s;
                pos++;
            } else {
                return pos + (sum + s - playlistpos);
            }
        }
        return pos;
    }

    // there are multiple songs in one position if it is an album
    protected int getPlaylistPosition(final int listto) {
        return getPlaylistPositions(0, listto);
    }

    protected int getPlaylistPositions(final int listfrom, final int listto) {
        int pos = 0;
        int to = Math.min(listto, mSongList.size());
        for (int i = listfrom; i < to; i++) {
            pos += mSongList.get(i).size();
        }
        return pos;
    }


    /**
     * Update the current playlist fragment.
     *
     * @param forcePlayingIDRefresh Force the current track to refresh.
     */
    @Override
    void update(final boolean forcePlayingIDRefresh) {
        update(forcePlayingIDRefresh, -1);
    }

    void update(final boolean forcePlayingIDRefresh, final int jumpTo) {
        collapsedAlbums = settings.getBoolean("collapseAlbums", false);
        // Save the scroll bar position to restore it after update
        final MPDPlaylist playlist = mApp.getMPD().getPlaylist();
        final List<Music> musics = playlist.getMusicList();
        final ArrayList<AbstractPlaylistMusic> newSongList = new ArrayList<>(musics.size());
        // Save the scroll bar position to restore it after update
        final int firstVisibleElementIndex = mList.getFirstVisiblePosition();
        View firstVisibleItem = mList.getChildAt(0);
        final int firstVisiblePosition = (firstVisibleItem != null) ? firstVisibleItem.getTop() : 0;

        if (mLastPlayingID == -1 || forcePlayingIDRefresh) {
            mLastPlayingID = mApp.getMPD().getStatus().getSongId();
        }

        // The position in the song list of the currently played song
        int listPlayingID = -1;

        // collect albums
        playlistAlbums = new ArrayList<PlaylistAlbum>();
        PlaylistAlbum plalbum = null;

        // Copy list to avoid concurrent exception
        for (final Music music : new ArrayList<>(musics)) {
            if (music == null) {
                continue;
            }
            AlbumInfo albuminfo = new AlbumInfo(music);
            if (plalbum == null || !albuminfo.equals(plalbum.getAlbumInfo())) {
                if (plalbum != null) {
                    playlistAlbums.add(plalbum);
                }
                plalbum = new PlaylistAlbum(albuminfo);
            }
            if (plalbum != null) {
                plalbum.add(music);
                if (music.getSongId() == mLastPlayingID) {
                    plalbum.setPlaying(true);
                }
            }
        }
        if (plalbum != null) { // remaining
            playlistAlbums.add(plalbum);
        }

        for (PlaylistAlbum a : playlistAlbums) {
            // add album item if not playing
            if (collapsedAlbums && !a.isPlaying() && a.size() > 1
                    && expandedAlbum != a.getSongId()) {
                if (mFilter != null && !a.hasText(mFilter)) {
                    continue;
                }
                newSongList.add(a);
            } else { // else add all songs
                for (Music music : a.getMusic()) {
                    final AbstractPlaylistMusic item;
                    if (music.isStream()) {
                        item = new PlaylistStream(music);
                    } else {
                        item = new PlaylistSong(music);
                    }

                    if (mFilter != null) {
                        if (isFiltered(item.getAlbumArtistName()) || isFiltered(item.getAlbumName())
                                || isFiltered(item.getTitle())) {
                            continue;
                        }
                    }

                    if (item.getSongId() == mLastPlayingID) {
                        if (mLightTheme) {
                            item.setCurrentSongIconRefID(R.drawable.ic_media_play_light);
                        } else {
                            item.setCurrentSongIconRefID(R.drawable.ic_media_play);
                        }

                        item.setCurrentSongIconRefID(mLightTheme ? R.drawable.ic_media_play_light
                                : R.drawable.ic_media_play);
                        /**
                         * Lie a little. Scroll to the previous
                         * song than the one playing.  That way it
                         * shows that there are other songs before
                         * it.
                         */
                        listPlayingID = newSongList.size() - 1;
                    } else {
                        item.setCurrentSongIconRefID(0);
                    }
                    // show only songs of playing album
                    newSongList.add(item);
                }
            }
        }
        updateScrollbar(newSongList, jumpTo >= 0 ? jumpTo : listPlayingID);
    }

}
