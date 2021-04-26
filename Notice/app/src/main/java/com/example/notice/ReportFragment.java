package com.example.notice;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.MPPointF;
import com.github.mikephil.charting.utils.Utils;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.Nullable;

public class ReportFragment extends Fragment {
    private LineChart lineChart;
    PieChart pieChart;
    ViewGroup rootView;
    long now ;
    Date datt;
    SimpleDateFormat sdfNow;
    String formatDate;
    Integer nowMonth;
    Integer[] dari=new Integer[6];
    Integer[] sixMonth=new Integer[6];
    Integer[] sixData ;
    private FirebaseDatabase mDatabase;
    private DatabaseReference mReference;
    private ChildEventListener mChild;
    private int month,date;
   // private ListView listView;
   // private ArrayAdapter<String> adapter;
  //  List<Object> Array = new ArrayList<Object>();
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        rootView = (ViewGroup) inflater.inflate(R.layout.frag_report, container, false);
        dari =new Integer[]{0,0,0,0,0,0};
        pieChart = (PieChart)rootView.findViewById(R.id.piechart);
        sixData =new Integer[]{0,0,0,0,0,0};
        //지금 몇월인지 ...
        now = System.currentTimeMillis();
        datt = new Date(now);
        // 시간을 나타냇 포맷을 정한다
        sdfNow = new SimpleDateFormat("MM");
        // nowDate 변수에 값을 저장한다.
        formatDate = sdfNow.format(datt);
        nowMonth = Integer.parseInt(formatDate);
       // listView = (ListView) rootView.findViewById(R.id.listviewmsg);
        lineChart = (LineChart)rootView.findViewById(R.id.chart);
        initDatabase();
     //   adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_dropdown_item_1line, new ArrayList<String>());
      //  listView.setAdapter(adapter);

        //현재 달부터 최근 6개월 계산
        for(int i=0;i<6;i++)
        {
            int temp = nowMonth-(i);
            if(temp<1)
                temp=temp+12;
            sixMonth[i]=temp;
        }

        mReference = mDatabase.getReference("person_list"); // 변경값을 확인할 child 이름
        mReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
           //     adapter.clear();
                for (DataSnapshot messageData : dataSnapshot.getChildren()) {
                    String msg2 = messageData.getValue().toString();

                    String[] spsp = msg2.split("@");
                    if (Integer.parseInt(spsp[0]) == 1) {
                        String[] result = spsp[1].split(" ");
                        String[] aaa = result[0].split("/");
                        month = Integer.parseInt(aaa[0]);
                        date = Integer.parseInt(aaa[1]);
                        for (int i = 0; i < 6; i++) {
                            if (sixMonth[i] == month)
                                sixData[i]++;
                        }
                        Log.d("jinajina", "month " + month + " date" + date + "\n");

                      //  Array.add(msg2);
                      //  adapter.add(msg2);
                        Log.d("jinajina", "1");

                    } else if (Integer.parseInt(spsp[0]) == 2) {
                        if (Integer.parseInt(spsp[1]) == 1) { //성수대교
                                dari[0]++;
                        }
                        else if(Integer.parseInt(spsp[1]) == 2) { //마포대교
                            dari[1]++;

                        }
                        else if(Integer.parseInt(spsp[1]) == 4) { //청담대교
                            dari[2]++;

                        }
                        else if(Integer.parseInt(spsp[1]) == 3) { //천호대교
                            dari[3]++;

                        }
                        else if(Integer.parseInt(spsp[1]) == 5) { //천호대교
                            dari[4]++;

                        }
                        else if(Integer.parseInt(spsp[1]) == 6) { //천호대교
                            dari[5]++;

                        }
                    }
                }
/*
                    Array.add("성수대교 : " + dari[0]+" 명");
                Array.add("청담대교 : " + dari[1]+" 명");
                Array.add("천호대교 : " + dari[2]+" 명");
                Array.add("마포대교 : " + dari[3]+" 명");
                Array.add("영동대교 : " + dari[4]+" 명");
                Array.add("잠수교 : " + dari[5]+" 명");

                adapter.add("성수대교 : " + dari[0]+" 명");
                adapter.add("청담대교 : " + dari[1]+" 명");
                adapter.add("천호대교 : " + dari[2]+" 명");
                adapter.add("마포대교 : " + dari[3]+" 명");
                adapter.add("영동대교 : " + dari[4]+" 명");
                adapter.add("잠수교 : " + dari[5]+" 명");
                adapter.notifyDataSetChanged();
                listView.setSelection(adapter.getCount() - 1);

 */

                list();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });




        return rootView;
    }
    private void list()
    {      List<Entry> entries = new ArrayList<>();
        entries.add(new Entry(sixMonth[5], sixData[5]));
        entries.add(new Entry(sixMonth[4], sixData[4]));
        entries.add(new Entry(sixMonth[3], sixData[3]));
        entries.add(new Entry(sixMonth[2], sixData[2]));
        entries.add(new Entry(sixMonth[1], sixData[1]));
        entries.add(new Entry(sixMonth[0], sixData[0]));
        for(int i=0;i<6;i++)
            Log.d("jinjin"," "+sixMonth[i]+" : "+sixData[i]);
        LineDataSet lineDataSet = new LineDataSet(entries, "투신자 수");
        lineDataSet.setLineWidth(5);
        lineDataSet.setCircleRadius(10);
        lineDataSet.setCircleColor(Color.parseColor("#FFA1B4DC"));
        lineDataSet.setCircleColorHole(Color.BLUE);
        lineDataSet.setColor(Color.parseColor("#FFA1B4DC"));
        lineDataSet.setDrawCircleHole(true);
        lineDataSet.setDrawCircles(true);
        lineDataSet.setDrawHorizontalHighlightIndicator(false);
        lineDataSet.setDrawHighlightIndicators(false);
        lineDataSet.setDrawValues(false);

        LineData lineData = new LineData(lineDataSet);
        lineChart.setData(lineData);

        XAxis xAxis = lineChart.getXAxis();
        //
        //  xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.BLACK);
        // xAxis.enableGridDashedLine(7, 24, 0);
        String[] sixMonthString= new String[6];
        for(int i=0;i<6;i++)
            sixMonthString[i]=Integer.toString(sixMonth[5-i]);

        // xAxis.setValueFormatter(new IndexAxisValueFormatter(sixMonthString));
        xAxis.setGranularity(1f);
        xAxis.setCenterAxisLabels(false);
        xAxis.setEnabled(true);
        xAxis.setDrawGridLines(false);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        YAxis yLAxis = lineChart.getAxisLeft();
        yLAxis.setGranularity(1f);
        yLAxis.setTextColor(Color.BLACK);

        YAxis yRAxis = lineChart.getAxisRight();
        yRAxis.setDrawLabels(false);
        yRAxis.setDrawAxisLine(false);
        yRAxis.setDrawGridLines(false);

        Description description = new Description();
        description.setText("최근 6개월");

        lineChart.setDoubleTapToZoomEnabled(false);
        lineChart.setDrawGridBackground(false);
        lineChart.setDescription(description);
        lineChart.animateY(2000, Easing.EasingOption.EaseInCubic);
        lineChart.invalidate();

        MyMarkerView marker = new MyMarkerView(getActivity(),R.layout.markerviewtext);
        marker.setChartView(lineChart);
        lineChart.setMarker(marker);



        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5,10,5,5);

        pieChart.setDragDecelerationFrictionCoef(0.95f);

        pieChart.setDrawHoleEnabled(false);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleRadius(61f);

        ArrayList<PieEntry> yValues = new ArrayList<PieEntry>();

     //   yValues.add(new PieEntry(34f,"천호대교"));
        yValues.add(new PieEntry(23f,"영동대교"));
        yValues.add(new PieEntry(14f,"마포대교"));
        yValues.add(new PieEntry(35f,"성수대교"));
        yValues.add(new PieEntry(40f,"잠수교"));
        yValues.add(new PieEntry(40f,"청담대교"));

        Description description2 = new Description();
      //  description2.setText("세계 국가"); //라벨
       // description2.setTextSize(15);
        //pieChart.setDescription(description);

        pieChart.animateY(1000, Easing.EasingOption.EaseInOutCubic); //애니메이션

        PieDataSet dataSet = new PieDataSet(yValues,"다리");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);
        dataSet.setColors(ColorTemplate.JOYFUL_COLORS);

        PieData data = new PieData((dataSet));
        data.setValueTextSize(10f);
        data.setValueTextColor(Color.YELLOW);

        pieChart.setData(data);



    }
    private void initDatabase() {

        mDatabase = FirebaseDatabase.getInstance();

        mReference = mDatabase.getReference("log");
        mReference.child("log").setValue("check");

        mChild = new ChildEventListener() {

            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        mReference.addChildEventListener(mChild);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mReference.removeEventListener(mChild);
    }



    public class MyMarkerView extends MarkerView {

        private TextView tvContent;

        public MyMarkerView(Context context, int layoutResource) {
            super(context, layoutResource);

            tvContent = (TextView)findViewById(R.id.tvContent);
        }

        // callbacks everytime the MarkerView is redrawn, can be used to update the
        // content (user-interface)
        @Override
        public void refreshContent(Entry e, Highlight highlight) {

            if (e instanceof CandleEntry) {

                CandleEntry ce = (CandleEntry) e;

                tvContent.setText("" + Utils.formatNumber(ce.getHigh(), 0, true));
            } else {

                tvContent.setText("" + Utils.formatNumber(e.getY(), 0, true));
            }

            super.refreshContent(e, highlight);
        }

        @Override
        public MPPointF getOffset() {
            return new MPPointF(-(getWidth() / 2), -getHeight());
        }
    }

}