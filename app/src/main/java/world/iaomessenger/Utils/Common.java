package world.iaomessenger.Utils;

import retrofit2.Retrofit;
import world.iaomessenger.Retrofit.IIAOMessengerAPI;
import world.iaomessenger.Retrofit.RetrofitClient;

public class Common {

    private static final String BASE_URL = "http://10.0.2.2/iaomessenger/";

    public static IIAOMessengerAPI getAPI() {

        return RetrofitClient.getClient(BASE_URL).create(IIAOMessengerAPI.class);

    }
}
