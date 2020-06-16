package world.iaomessenger.Retrofit;

import retrofit2.Call;
import retrofit2.http.Field;
import world.iaomessenger.Model.CheckUserResponse;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface IIAOMessengerAPI {

    @FormUrlEncoded
    @POST("checkuser.php")
    Call<CheckUserResponse> checkUserExists(@Field("user_phone") String user_phone);

    @FormUrlEncoded
    @POST("register.php")
    Call<CheckUserResponse> registerNewUser(@Field("user_username") String user_username,
                                            @Field("user_name") String user_name,
                                            @Field("user_email") String user_email,
                                            @Field("user_password") String user_password,
                                            @Field("user_statut") String user_statut,
                                            @Field("user_phone") String user_phone);

}
