using UnityEngine;

public class WebConfigBridge : MonoBehaviour
{
    public static string ApiBaseUrl { get; private set; } = "http://localhost:8080/api/v1";

    public void ReceiveApiBaseUrl(string apiBaseUrl)
    {
        if (!string.IsNullOrWhiteSpace(apiBaseUrl))
        {
            ApiBaseUrl = apiBaseUrl.Trim().TrimEnd('/');
        }

        Debug.Log($"[WebConfigBridge] API Base URL received: {ApiBaseUrl}");
    }
}
