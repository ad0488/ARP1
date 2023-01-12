package org.tensorflow.lite.examples.objectdetection;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.DataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IFillFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.model.GradientColor;

import java.util.ArrayList;
import java.util.List;

public class GraphActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        LineChart chartFom = findViewById(R.id.chartFom);
        LineChart chartPerclos = findViewById(R.id.chartPerclos);
        LineChart chartDrowsy = findViewById(R.id.chartDrowsy);

        TinyDB tinydb = new TinyDB(getApplicationContext());

        ArrayList<Double> fom = tinydb.getListDouble("fom");
        ArrayList<Double> perclos = tinydb.getListDouble("perclos");
        ArrayList<Double> drowsy = tinydb.getListDouble("drowsy");

        createGraph(chartFom, fom, "FOM");
        createGraph(chartPerclos, perclos, "PERCLOS");
        createGraph(chartDrowsy, drowsy, "Drowsiness Level");
    }

    private void createGraph(LineChart chart, ArrayList<Double> values, String label ){
        List<Entry> entries = new ArrayList<>();
        int i = 0;
        for (Double value : values) {
            entries.add(new Entry(i + 1, value.floatValue()));
            i++;
        }
        LineDataSet dataSetFom = new LineDataSet(entries, label);
        dataSetFom.setColor(Color.RED);
        dataSetFom.setDrawCircles(false);
        dataSetFom.setDrawValues(false);
        List<ILineDataSet> ILineDataSetFom = new ArrayList<>();
        ILineDataSetFom.add(dataSetFom);
        LineData lineDataFom = new LineData(ILineDataSetFom);
        chart.setData(lineDataFom);
        chart.setNoDataText("No chart data");
        chart.getDescription().setEnabled(false);
        chart.setVisibleXRangeMaximum(100);
        chart.invalidate();
    }
}