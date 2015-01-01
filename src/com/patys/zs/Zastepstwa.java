package com.patys.zs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;


public class Zastepstwa extends Activity {

	private static String file_url = "http://zs.ketrzyn.pl/plan/zast.xls";
	private String file_path;
	
	public static Boolean downloaded;
	
	private ListView excel_view;
	
	private ProgressDialog p_dialog;
	public static final int progress_bar_type = 0;
	private Handler handler;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_zastepstwa);
		
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy); 

        file_path = this.getFilesDir().toString()
    			+ "/zast.xls";
        
        downloaded = false;
	    new DownloadFileFromURL().execute(file_url);
	    final File file = new File(file_path);
	    
	    /*
    	handler = new Handler();
    	handler.post(new Runnable(){

    		@Override
            public void run() {

    		    if(downloaded == true)      
    		    	System.out.println("Yep dziala");
    		    else
    		    	System.out.println("nope, shhit");
    		    if(file.exists() == true)      
    		    	System.out.println("Yep dziala plik");
    		    else
    		    	System.out.println("nope, shhit plik");
    			// upadte textView here

                handler.postDelayed(this,500); // set time here to refresh textView
            }
        });*/
		String excel_full = "";
	    if(file.exists())
	    {
		    List<String> excel_dataList;
		    try {
				excel_dataList = read();
				for (int i = 0; i < excel_dataList.size(); i++) {
				   String item = excel_dataList.get(i);
				   if(item.contains("#NEXT"))
				   {
					   excel_full += "\n";
				   }else
				   {
					   excel_full += item;
				   }
				}

			} catch (IOException e) {
				e.printStackTrace(); 
			}
		    
	    }
        TextView textView = (TextView) findViewById(R.id.excel_text); 
        textView.setText(excel_full);
	    
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.zastepstwa, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	

	/**
	 * Showing Dialog
	 * */
	
	@Override
	protected Dialog onCreateDialog(int id) {
	    switch (id) {
	    case progress_bar_type: // we set this to 0
	        p_dialog = new ProgressDialog(this);
	        p_dialog.setMessage("Syncing with school server ...");
	        p_dialog.setIndeterminate(true);
	        p_dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	        p_dialog.setCancelable(true);
	        p_dialog.show();
	        return p_dialog;
	    default:
	        return null;
	    }
	}
	
	/**
	 * Background Async Task to download file
	 * */
	class DownloadFileFromURL extends AsyncTask<String, String, String> {
	    
	    /**
	     * Before starting background thread Show Progress Bar Dialog
	     * */
	    @Override
	    protected void onPreExecute() {
	        super.onPreExecute();
	        showDialog(progress_bar_type);
	    }

		/**
	     * Downloading file in background thread
	     * */
	    @Override
	    protected String doInBackground(String... sUrl) {
	        InputStream input = null;
	        OutputStream output = null;
	        HttpURLConnection connection = null;
	        try {
	            URL url = new URL(sUrl[0]);
	            connection = (HttpURLConnection) url.openConnection();
	            connection.connect();

	            // expect HTTP 200 OK, so we don't mistakenly save error report
	            // instead of the file
	            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
	                return "Server returned HTTP " + connection.getResponseCode()
	                        + " " + connection.getResponseMessage();
	            }

	            // this will be useful to display download percentage
	            // might be -1: server did not report the length
	            int fileLength = connection.getContentLength();

	            // download the file
	            input = connection.getInputStream();
	            output = new FileOutputStream("zast.xls");

	            byte data[] = new byte[4096];
	            long total = 0;
	            int count;
	            while ((count = input.read(data)) != -1) {
	                // allow canceling with back button
	                if (isCancelled()) {
	                    input.close();
	                    return null;
	                }
	                total += count;
	                // publishing the progress....
	                if (fileLength > 0) // only if total length is known
	                    publishProgress(""+(int) (total * 100 / fileLength));
	                output.write(data, 0, count);
	            }
	        } catch (Exception e) {
	            return e.toString();
	        } finally {
	            try {
	                if (output != null)
	                    output.close();
	                if (input != null)
	                    input.close();
	            } catch (IOException ignored) {
	            }

	            if (connection != null)
	                connection.disconnect();
	        }
	        return null;
	    }

	    /**
	     * Updating progress bar
	     * */
	    protected void onProgressUpdate(String... progress) {
	        // setting progress percentage
	        p_dialog.setProgress(Integer.parseInt(progress[0]));
	    }

	    /**
	     * After completing background task Dismiss the progress dialog
	     * **/
	    @Override
	    protected void onPostExecute(String file_url) {
	        // dismiss the dialog after the file was downloaded
	        dismissDialog(progress_bar_type);
	        downloaded = true;

	    }
	}
	
	public List<String> read() throws IOException  {
	    List<String> resultSet = new ArrayList<String>();
	    
	    File inputWorkbook = new File(file_path);
	    
	    if(inputWorkbook.exists()){
	        Workbook w;
	        try {
	            w = Workbook.getWorkbook(inputWorkbook);
	            // Get the first sheet
	            Sheet sheet = w.getSheet(0);
	            // Loop over column and lines
	            for (int j = 0; j < sheet.getRows(); j++) {
                    for (int i = 0; i < sheet.getColumns(); i++) {
                        Cell cel = sheet.getCell(i, j);
                        String contents = cel.getContents();

                        // ustawianie tekstu
                        if(i == 0)
                        {
                            if(contents == null || contents.length() == 0)
                            	resultSet.add("          ");
                            else if(contents.length() < 10)
                            {
                            	while(contents.length() != 10)
                            	{
                            		contents += " ";
                            	}
                            }
                        	resultSet.add(contents);
                        }
                        else if(i == 1)
                        {
                            if(contents == null || contents.length() == 0)
                            	resultSet.add("               ");
                            else if(contents.length() < 15)
                            {
                            	while(contents.length() != 15)
                            	{
                            		contents += " ";
                            	}
                            }
                        	resultSet.add(contents);
                         } 
                        else if(i == 2)
                        {
                            if(contents == null || contents.length() == 0)
                            	resultSet.add("                    ");
                            else if(contents.length() < 20)
                            {
                            	while(contents.length() != 20)
                            	{
                            		contents += " ";
                            	}
                            }
                        	resultSet.add(contents);
                        }
                        else if(i == 3)
                        {
                            if(contents == null || contents.length() == 0)
                            	resultSet.add("                    ");
                            else if(contents.length() < 20)
                            {
                            	while(contents.length() != 20)
                            	{
                            		contents += " "; 
                            	}
                            }
                        	resultSet.add(contents);
                        }
                        else
                        	resultSet.add(contents);
                        	
                        
                        
	                }
                    resultSet.add("#NEXT");
	                continue;
	            }
	        } catch (BiffException e) {
	            e.printStackTrace();
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }
	    else
	    {
	        resultSet.add("File not found..!");
	    }
	    if(resultSet.size()==0){
	        resultSet.add("Data not found..!");
	    }
	    return resultSet;
	}
}
