package pozzo.apps.travelweather.ui.activity;

import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

import pozzo.apps.travelweather.R;
import pozzo.apps.travelweather.business.LocationBusiness;
import pozzo.apps.travelweather.network.ApiFactory;
import retrofit.client.Response;
import retrofit.mime.TypedByteArray;

/**
 * Atividade para exibir o mapa.
 */
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private LocationBusiness locationBusiness;

    private GoogleMap mMap;
    private EditText ePlace;
    private LatLng startPosition;
    private LatLng finishPosition;

    {
        locationBusiness = new LocationBusiness();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        ePlace = (EditText) findViewById(R.id.ePlace);
        ePlace.setOnEditorActionListener(onPlace);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        pointToCurrentLocation();

        mMap.setOnMapClickListener(onMapClick);
    }

    /**
     * Gera o ponto incial.
     */
    private void pointToCurrentLocation() {
        Location location = locationBusiness.getCurrentLocation(this);
        if(location != null) {
            setStartPosition(new LatLng(location.getLatitude(), location.getLongitude()));
        }
    }

    /**
     * @param startPosition Nova posicao inicial.
     */
    private void setStartPosition(LatLng startPosition) {
        this.startPosition = startPosition;
        if(startPosition != null) {
            addMark(startPosition, R.string.startPosition);
            pointMapTo(startPosition);
        }
    }

    /**
     * O dado mapa sera apontado para a dada posicao.
     */
    private void pointMapTo(LatLng latLng) {
        //TODO este zoom deve ser esperto o suficiente para pegar todos os pontos marcados.
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f));
    }

    /**
     * Adiciona marcacao no mapa.
     */
    private void addMark(LatLng latLng, int text) {
        mMap.addMarker(new MarkerOptions().position(latLng).title(getString(text)));
    }

    /**
     * Define um novo ponto final.
     */
    private void setFinish(LatLng finishPosition) {
        this.finishPosition = finishPosition;
        if(finishPosition != null) {
            addMark(finishPosition, R.string.finishPosition);
            pointMapTo(finishPosition);
            updateTrack(mMap);
        }
    }

    /**
     * Limpa tudo que estiver no mapa.
     */
    private void clear() {
        mMap.clear();
    }

    /**
     * Atualiza tragetoria.
     */
    private void updateTrack(final GoogleMap googleMap) {
        if(startPosition == null || finishPosition == null)
            return;

        new AsyncTask<Void, Void, PolylineOptions>() {
            @Override
            protected PolylineOptions doInBackground(Void... params) {
                ArrayList<LatLng> directionPoint =
                        locationBusiness.getDirections(startPosition, finishPosition);

                PolylineOptions rectLine = new PolylineOptions().width(3).color(Color.RED);
                for(int i = 0 ; i < directionPoint.size() ; i++) {
                    rectLine.add(directionPoint.get(i));
                }
                return rectLine;
            }

            @Override
            protected void onPostExecute(PolylineOptions rectLine) {
                googleMap.addPolyline(rectLine);
                LatLng mid = new LatLng(
                        (startPosition.latitude + finishPosition.latitude) / 2.0,
                        (startPosition.longitude + finishPosition.longitude) / 2.0);
                pointMapTo(mid);
            }
        }.execute();
    }

    /**
     * Usuario quer nos indicar alguma coisa \o/.
     */
    private GoogleMap.OnMapClickListener onMapClick = new GoogleMap.OnMapClickListener() {
        @Override
        public void onMapClick(LatLng latLng) {
            clear();
            setStartPosition(startPosition);
            setFinish(latLng);
        }
    };

    private TextView.OnEditorActionListener onPlace = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			queryWeather(v.getText().toString());
			return true;
        }
    };

	private void queryWeather(final String location) {
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				String query = "select * from weather.forecast where woeid in " +
						"(select woeid from geo.places(1) where text=\"" + location + "\")";
				Response response = ApiFactory.getInstance().getYahooWather().forecast(query);
				System.out.println(new String(((TypedByteArray) response.getBody()).getBytes()));
				return null;
			}
		}.execute();
	}
}
