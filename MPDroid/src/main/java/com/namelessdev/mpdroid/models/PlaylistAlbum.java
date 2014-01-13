package com.namelessdev.mpdroid.models;

import com.anpmech.mpd.item.Album;
import com.anpmech.mpd.item.Artist;
import com.anpmech.mpd.item.Music;
import com.namelessdev.mpdroid.helpers.AlbumInfo;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/*
 * Playlist representation of one album
 * to show one line for a sequence of tracks belonging to one album
 */
public class PlaylistAlbum extends AbstractPlaylistMusic {

    int lastPlayingID = -1;

    boolean isPlaying = false;

    int firstSongId = -1;

    List<Music> music;

    Album album = null;

    AlbumInfo ainfo;

    static final Music nullMusic = new Music("");

    public PlaylistAlbum(AlbumInfo ainfo) {
        super(nullMusic);
        this.ainfo = ainfo;
        music = new ArrayList<Music>();
    }

    public PlaylistAlbum(List<Music> music) {
        super(nullMusic);
        this.music = music;
        Music m = music.get(0);
        this.album = m.getAlbum();
    }

    public void add(Music m) {
        if (music.size() == 0) {
            firstSongId = m.getSongId();
        }
        music.add(m);
    }

    public List<Music> getMusic() {
        return music;
    }

    // identify by first song id
    public int getSongId() {
        return firstSongId;
    }

    public boolean hasSongId(int songId) {
        for (Music m : music) {
            if (m.getSongId() == songId) {
                return true;
            }
        }
        return false;
    }

    // all ids
    public List<Integer> getSongIds() {
        List<Integer> l = new ArrayList<Integer>();
        for (Music m : music) {
            l.add(m.getSongId());
        }
        return l;
    }

    public void setLastPlayingID(int id) {
        lastPlayingID = id;
    }

    public int getLastPlayingID() {
        return lastPlayingID;
    }

    public void setPlaying(boolean p) {
        isPlaying = true;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public AlbumInfo getAlbumInfo() {
        return ainfo;
    }

    public String getPlayListMainLine() {
        if (ainfo == null) {
            return "Empty";
        }
        String album = ainfo.getAlbumName();
        if (album == null) {
            album = "";
        }
        return "(" + music.size() + ") [" + album + "]";
    }

    public String getPlaylistSubLine() {
        if (ainfo == null) {
            return "Empty";
        }
        String artist = ainfo.getArtistName();
        if (artist == null) {
            artist = "";
        }
        return "[" + artist + "]";
    }

    public long size() {
        return music.size();
    }

    @NotNull
    @Override
    public String getFullPath() {
        return "";
    }

    public String getArtistName() {
        return ainfo == null ? "null" : ainfo.getArtistName();
    }

    public String getAlbumName() {
        return ainfo == null ? "null" : ainfo.getAlbumName();
    }

    public Album getAlbum() {
        return music.get(0).getAlbum();
    }

    public Artist getArtist() {
        return music.get(0).getArtist();
    }

    public String getAlbumArtistName() {
        return music.get(0).getAlbumArtistName();
    }

    public Artist getAlbumArtist() {
        return music.get(0).getAlbumArtist();
    }

    public boolean hasText(String filter) {
        if ((getArtistName() != null ? getArtistName() : "")
                .toLowerCase(Locale.getDefault()).contains(filter) ||
                (getAlbumName() != null ? getAlbumName() : "")
                        .toLowerCase(Locale.getDefault()).contains(filter)) {
            return true;
        }
        for (Music m : music) {
            if ((m.getTitle() != null ? m.getTitle() : "")
                    .toLowerCase(Locale.getDefault()).contains(filter)) {
                return true;
            }
        }
        return false;
    }

    public String toString() {
        return "PlAlbum: " + ainfo.getArtistName() + "/" + ainfo.getAlbumName() + " (" + music
                .size() + ")";
    }

}
