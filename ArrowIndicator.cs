using UnityEngine;
using UnityEngine.UI;

public class ArrowIndicator : MonoBehaviour
{
    public Transform player;
    public Image arrowImage;

    void Update()
    {
        GameObject target = FindClosestCode();
        if (target == null) return;

        Vector2 dir = target.transform.position - player.position;

        float angle = Mathf.Atan2(dir.y, dir.x) * Mathf.Rad2Deg;

        // 화살표 이미지가 위를 보고 있다면 -90
        arrowImage.rectTransform.rotation = Quaternion.Euler(0, 0, angle - 90f);
    }

    GameObject FindClosestCode()
    {
        GameObject[] codes = GameObject.FindGameObjectsWithTag("Code");
        GameObject closest = null;
        float minDist = Mathf.Infinity;

        foreach (GameObject code in codes)
        {
            float dist = Vector2.Distance(player.position, code.transform.position);
            if (dist < minDist)
            {
                minDist = dist;
                closest = code;
            }
        }
        return closest;
    }
}
