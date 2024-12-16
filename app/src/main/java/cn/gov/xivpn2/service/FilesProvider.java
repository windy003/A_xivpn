package cn.gov.xivpn2.service;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.CancellationSignal;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsContract.Root;
import android.provider.DocumentsProvider;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;

import cn.gov.xivpn2.R;

public class FilesProvider extends DocumentsProvider {

    private static final String[] DEFAULT_ROOT_PROJECTION =
            new String[]{Root.COLUMN_ROOT_ID, Root.COLUMN_ICON, Root.COLUMN_TITLE, Root.COLUMN_DOCUMENT_ID, Root.COLUMN_FLAGS};

    private static final String[] DEFAULT_DOCUMENT_PROJECTION = new
            String[]{Document.COLUMN_DOCUMENT_ID, Document.COLUMN_MIME_TYPE,
            Document.COLUMN_DISPLAY_NAME, Document.COLUMN_LAST_MODIFIED,
            Document.COLUMN_FLAGS, Document.COLUMN_SIZE,};

    private File base;

    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException {
        if (projection == null) projection = DEFAULT_ROOT_PROJECTION;
        MatrixCursor matrixCursor = new MatrixCursor(projection);

        MatrixCursor.RowBuilder rowBuilder = matrixCursor.newRow();
        rowBuilder.add(Root.COLUMN_ROOT_ID, "files");
        rowBuilder.add(Root.COLUMN_FLAGS, Root.FLAG_SUPPORTS_IS_CHILD);
        rowBuilder.add(Root.COLUMN_DOCUMENT_ID, "/");
        rowBuilder.add(Root.COLUMN_ICON, R.mipmap.ic_launcher);
        rowBuilder.add(Root.COLUMN_TITLE, "XiVPN");

        return matrixCursor;
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
        if (projection == null) projection = DEFAULT_DOCUMENT_PROJECTION;

        if (documentId.contains("..")) {
            throw new FileNotFoundException("illegal file name");
        }

        File file = new File(base.getAbsolutePath() + documentId);

        MatrixCursor cursor = new MatrixCursor(projection);
        MatrixCursor.RowBuilder row = cursor.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, documentId);
        if (file.isDirectory()) {
            row.add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR);
        } else {
            row.add(Document.COLUMN_MIME_TYPE, "text/plain");
        }

        row.add(Document.COLUMN_LAST_MODIFIED, file.lastModified());
        row.add(Document.COLUMN_FLAGS, Document.FLAG_SUPPORTS_DELETE);
        row.add(Document.COLUMN_SIZE, file.length());
        return cursor;
    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder) throws FileNotFoundException {
        if (projection == null) projection = DEFAULT_DOCUMENT_PROJECTION;

        if (parentDocumentId.contains("..")) {
            throw new FileNotFoundException("illegal file name");
        }

        MatrixCursor cursor = new MatrixCursor(projection);

        File f = new File(base.getAbsolutePath() + parentDocumentId);

        for (File file : Objects.requireNonNull(f.listFiles())) {

            MatrixCursor.RowBuilder row = cursor.newRow();
            row.add(Document.COLUMN_DOCUMENT_ID, file.getAbsolutePath().substring(base.getAbsolutePath().length()));
            row.add(Document.COLUMN_LAST_MODIFIED, file.lastModified());
            row.add(Document.COLUMN_FLAGS, Document.FLAG_SUPPORTS_DELETE);
            row.add(Document.COLUMN_SIZE, file.length());
            row.add(Document.COLUMN_DISPLAY_NAME, file.getName());

            if (file.isDirectory()) {
                row.add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR);
            } else {
                row.add(Document.COLUMN_MIME_TYPE, "text/plain");
            }
        }

        return cursor;
    }

    @Override
    public boolean isChildDocument(String parentDocumentId, String documentId) {
        return documentId.startsWith(parentDocumentId);
    }

    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode, @Nullable CancellationSignal signal) throws FileNotFoundException {
        if (documentId.contains("..")) {
            throw new FileNotFoundException("illegal file name");
        }

        if (!mode.equals("r")){
            throw new FileNotFoundException("read only");
        }

        File file = new File(base.getAbsolutePath() + documentId);
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.parseMode(mode));
    }

    @Override
    public boolean onCreate() {
        base = new File(Objects.requireNonNull(getContext()).getDataDir(), "logs");
        base.mkdirs();
        return true;
    }
}
