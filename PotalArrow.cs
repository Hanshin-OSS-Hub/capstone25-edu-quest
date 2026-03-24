using UnityEngine;
using UnityEngine.UI;

public class PotalArrow : MonoBehaviour
{
    public Transform player;
    public Image arrowImage;
    public GameObject textObject; // 껐다 켰다 할 텍스트

    void Start()
    {
        // 1. 게임이 시작되면 일단 텍스트를 안 보이게 꺼둡니다.
        if (textObject != null)
        {
            textObject.SetActive(false);
        }
    }

    void Update()
    {
        GameObject target = FindClosestPotal();
        if (target == null) return;

        Vector2 dir = target.transform.position - player.position;

        float angle = Mathf.Atan2(dir.y, dir.x) * Mathf.Rad2Deg;

        // 화살표 이미지가 위를 보고 있다면 -90
        arrowImage.rectTransform.rotation = Quaternion.Euler(0, 0, angle - 90f);
    }

    GameObject FindClosestPotal()
    {
        GameObject[] potal = GameObject.FindGameObjectsWithTag("nextPotal");
        GameObject closest = null;
        float minDist = Mathf.Infinity;

        foreach (GameObject p in potal)
        {
            float dist = Vector2.Distance(player.position, p.transform.position);
            if (dist < minDist)
            {
                minDist = dist;
                closest = p;
            }
        }
        return closest;
    }
}
