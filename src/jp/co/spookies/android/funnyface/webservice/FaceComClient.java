package jp.co.spookies.android.funnyface.webservice;

import java.io.File;
import java.util.List;

import com.github.mhendred.face4j.DefaultFaceClient;
import com.github.mhendred.face4j.FaceClient;
import com.github.mhendred.face4j.exception.FaceClientException;
import com.github.mhendred.face4j.exception.FaceServerException;
import com.github.mhendred.face4j.model.Face;
import com.github.mhendred.face4j.model.Photo;

/**
 * face.comのＡＰＩを利用する
 */
public class FaceComClient {

    // http://developers.face.com/ からキーを取得してください。
    // FACECOMのAPI Key
    private static final String API_KEY = "23f903ae1db97da5c60e33a18b0f18b8";
    // FACECOMのAPI Secret
    private static final String API_SEC = "ed6c0edd3b6cf2898ffdc9c3098e5240";

    private FaceClient faceClient;

    public FaceComClient() {
        faceClient = new DefaultFaceClient(API_KEY, API_SEC);
    }

    /**
     * 写真から顔情報を取得
     * 
     * @param image
     * @return
     * @throws FaceClientException
     * @throws FaceServerException
     */
    public List<Face> getFaceData(File image) throws FaceClientException,
            FaceServerException {
        Photo photo = getPhotoData(image);
        return photo.getFaces();
    }

    /**
     * FaceAPIから写真情報を取得
     * 
     * @param image
     * @return
     * @throws FaceClientException
     * @throws FaceServerException
     */
    private Photo getPhotoData(File image) throws FaceClientException,
            FaceServerException {
        Photo photo = faceClient.detect(image);
        return photo;
    }

}
