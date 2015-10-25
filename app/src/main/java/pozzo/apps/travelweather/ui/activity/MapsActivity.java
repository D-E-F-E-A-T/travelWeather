package pozzo.apps.travelweather.ui.activity;

import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

import pozzo.apps.travelweather.R;
import pozzo.apps.travelweather.business.ForecastBusiness;
import pozzo.apps.travelweather.business.LocationBusiness;
import pozzo.apps.travelweather.helper.ForecastHelper;
import pozzo.apps.travelweather.model.Forecast;
import pozzo.apps.travelweather.model.Weather;

/**
 * Atividade para exibir o mapa.
 */
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private LocationBusiness locationBusiness;
    private ForecastBusiness forecastBusiness;

    private GoogleMap mMap;
    private LatLng startPosition;
    private LatLng finishPosition;

    {
        locationBusiness = new LocationBusiness();
        forecastBusiness = new ForecastBusiness();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

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
            queryWeather(startPosition);
            pointMapTo(startPosition);
        }
    }

    /**
     * O dado mapa sera apontado para a dada posicao.
     */
    private void pointMapTo(LatLng latLng) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f));
    }

    /**
     * O dado mapa sera apontado para a dada posicao.
     */
    private void pointMapTo(LatLngBounds latLng) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLng, 10));
    }

    /**
     * Adiciona marcacao no mapa.
     */
    private void addMark(LatLng latLng, String text, int resource) {
        mMap.addMarker(new MarkerOptions().position(latLng).title(text)
                .icon(BitmapDescriptorFactory.fromResource(resource)));
    }

    /**
     * Define um novo ponto final.
     */
    private void setFinish(LatLng finishPosition) {
        this.finishPosition = finishPosition;
        if(finishPosition != null) {
            queryWeather(finishPosition);
            pointMapTo(LatLngBounds.builder()
                    .include(startPosition).include(finishPosition).build());
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
                //Start jah possui
                LatLng lastForecast = directionPoint.get(0);
                for(int i = 0 ; i < directionPoint.size() ; i++) {
                    LatLng latLng = directionPoint.get(i);
                    rectLine.add(latLng);
                    //Um mod para nao checar em todos os pontos, sao muitos
                    if(i % 500 == 1 && isMinDistanceToForecast(latLng, lastForecast)) {
                        queryWeather(latLng);
                        lastForecast = latLng;
                    }
                }
                return rectLine;
            }

            @Override
            protected void onPostExecute(PolylineOptions rectLine) {
                googleMap.addPolyline(rectLine);
            }
        }.execute();
    }

    /**
     * @return true if distance is enough for a new forecast.
     */
    private boolean isMinDistanceToForecast(LatLng from, LatLng to) {
        double distance = Math.abs(from.latitude - to.latitude)
                + Math.abs(from.longitude - to.longitude);
        return distance > 0.7;
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

	private void queryWeather(final LatLng location) {
		new AsyncTask<Void, Void, Weather>() {
			@Override
			protected Weather doInBackground(Void... params) {
                return forecastBusiness.from(location, MapsActivity.this);
			}

            @Override
            protected void onPostExecute(Weather weather) {
                Forecast firstForecast = weather.getForecasts()[0];
                String message = firstForecast.getText();
                addMark(location, message, ForecastHelper.forecastIcon(firstForecast));
            }
        }.execute();
	}
}
