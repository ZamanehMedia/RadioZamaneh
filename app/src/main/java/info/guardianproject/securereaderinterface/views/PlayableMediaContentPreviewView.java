package info.guardianproject.securereaderinterface.views;

import android.view.View;
import android.widget.TextView;

import com.tinymission.rss.MediaContent;

import info.guardianproject.iocipher.File;

public interface PlayableMediaContentPreviewView
{
	public void pauseIfPlaying();
	public void setPlayPauseView(View playPauseView, TextView mediaStatusView);
}
