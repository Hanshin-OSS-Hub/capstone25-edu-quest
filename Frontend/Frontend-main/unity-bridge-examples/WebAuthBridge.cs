using UnityEngine;

public class WebAuthBridge : MonoBehaviour
{
    public static string AccessToken { get; private set; } = string.Empty;

    public void ReceiveAccessToken(string token)
    {
        AccessToken = token ?? string.Empty;
        Debug.Log($"[WebAuthBridge] Access token received. Has token: {!string.IsNullOrEmpty(AccessToken)}");
    }

    public static string GetAuthorizationHeader()
    {
        if (string.IsNullOrEmpty(AccessToken))
        {
            return string.Empty;
        }

        return $"Bearer {AccessToken}";
    }
}
