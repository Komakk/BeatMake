package com.anton.beatmake.fileexplorer;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.anton.beatmake.MainActivity;
import com.anton.beatmake.R;

import java.io.File;
import java.sql.Date;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

public class FileExplorer extends ListActivity {
    public static final String ID_EXTRA = "id";
    public static final String PATH_EXTRA = "path";

    private static final File rootDir = new File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC) + "/beatmake");
    private File currentDir;
    private FileArrayAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentDir = rootDir;
        fill(currentDir);
    }

    private void fill(File f) {
        File[]dirs = f.listFiles();
        this.setTitle("Current Dir: "+f.getName());
        List<Item> dir = new ArrayList<>();
        List<Item> fls = new ArrayList<>();
        try{
            for(File ff: dirs)
            {
                Date lastModDate = new Date(ff.lastModified());
                DateFormat formater = DateFormat.getDateTimeInstance();
                String date_modify = formater.format(lastModDate);
                if(ff.isDirectory()){


                    File[] fbuf = ff.listFiles();
                    int buf = 0;
                    if(fbuf != null){
                        buf = fbuf.length;
                    }
                    else buf = 0;
                    String num_item = String.valueOf(buf);
                    if(buf == 0) num_item = num_item + " item";
                    else num_item = num_item + " items";

                    //String formated = lastModDate.toString();
                    dir.add(new Item(ff.getName(),num_item,date_modify,ff.getAbsolutePath(),"directory_icon"));
                }
                else
                {
                    fls.add(new Item(ff.getName(),ff.length() + " Byte", date_modify, ff.getAbsolutePath(),"file_icon"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        dir.addAll(fls);
        if(!f.getName().equalsIgnoreCase(rootDir.getName()))
            dir.add(0,new Item("..","Parent Directory","",f.getParent(),"directory_up"));
        adapter = new FileArrayAdapter(FileExplorer.this, R.layout.list_row,dir);
        this.setListAdapter(adapter);
    }
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        // TODO Auto-generated method stub
        super.onListItemClick(l, v, position, id);
        Item o = adapter.getItem(position);
        assert o != null;
        if(o.getImage().equalsIgnoreCase("directory_icon")||o.getImage().equalsIgnoreCase("directory_up")){
            currentDir = new File(o.getPath());
            fill(currentDir);
        }
        else
        {
            onFileClick(o);
        }
    }
    private void onFileClick(Item o) {
        String extension = o.getPath().substring(o.getPath().lastIndexOf(".") + 1, o.getPath().length());
        if(extension.equals("wav") || extension.equals("WAV")) {
        Intent resultIntent = new Intent(getApplicationContext(), MainActivity.class);
        resultIntent.putExtra(PATH_EXTRA, o.getPath());
        resultIntent.putExtra(ID_EXTRA, getIntent().getIntExtra(ID_EXTRA, -1));
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
        } else {
            Toast.makeText(this,"Unsupported audio file format. Choose .wav", Toast.LENGTH_SHORT).show();
        }
    }
}
