package info.guardianproject.securereaderinterface.views;

import android.content.Context;
import com.tinymission.rss.MediaContent;
import info.guardianproject.iocipher.File;
import info.guardianproject.securereaderinterface.ui.MediaViewCollection;

public class ThumbnailMediaContentPreviewView extends ImageMediaContentPreviewView {
	public static final String LOGTAG = "ThumbnailMediaContentPreviewView";
	public static final boolean LOGGING = false;

	private MediaViewCollection mCollection;

	public ThumbnailMediaContentPreviewView(Context context, MediaViewCollection collection) {
		super(context);
		mCollection = collection;
	}

	@Override
	public void setMediaContent(MediaContent mediaContent, File mediaFile, java.io.File mediaFileNonVFS, boolean useThisThread) {
		super.setMediaContent(mediaContent, mediaFile, mediaFileNonVFS, useThisThread);
		for (MediaContentPreviewView view : mCollection.getViews()) {
			if (view instanceof AudioMediaContentPreviewView) {
				((AudioMediaContentPreviewView) view).setThumbnailMediaContent(mediaContent, mediaFile, mediaFileNonVFS, useThisThread);
			}
		}
	}
}