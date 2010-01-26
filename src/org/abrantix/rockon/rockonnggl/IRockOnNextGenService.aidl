package org.abrantix.rockon.rockonnggl;

import android.graphics.Bitmap;

interface IRockOnNextGenService
{
    void openFile(String path, boolean oneShot);
    void openFileAsync(String path);
    void open(in long [] list, int position);
    int getQueuePosition();
    boolean isPlaying();
    void stop();
    void pause();
    void play();
    void prev();
    void next();
    long duration();
    long position();
    long seek(long pos);
    String getTrackName();
    String getAlbumName();
    long getAlbumId();
    String getArtistName();
    long getArtistId();
    void enqueue(in long [] list, int action);
    long [] getQueue();
    long [] getOutstandingQueue();
    void moveQueueItem(int from, int to);
    void setQueuePosition(int index);
    String getPath();
    long getAudioId();
    void setPlaylistId(int playlistId);
    int	getPlaylistId();
    void setShuffleMode(int shufflemode);
    int getShuffleMode();
    void setRockOnShuffleMode(int shufflemode);
    int getRockOnShuffleMode();
    int removeTracks(int first, int last);
    int removeTrack(long id);
    void setRepeatMode(int repeatmode);
    int getRepeatMode();
    void setRockOnRepeatMode(int repeatmode);
    int getRockOnRepeatMode();
    int getMediaMountedCount();
}

