using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class ShowText : MonoBehaviour
{
    [Header("파이썬에서 Print를 사용을 하면 문자열을 나타낼 수 있어!")]
    public GameObject textObject; // 껐다 켰다 할 텍스트

    void Start()
    {
        // 1. 게임이 시작되면 일단 텍스트를 안 보이게 꺼둡니다.
        if (textObject != null)
        {
            textObject.SetActive(false);
        }
    }

    // 2. 플레이어가 오브젝트 근처(Trigger 범위)에 들어왔을 때
    void OnTriggerEnter2D(Collider2D collision)
    {
        // 들어온 대상의 태그가 "Player"라면?
        if (collision.CompareTag("Player"))
        {
            textObject.SetActive(true); // 텍스트 켜기!
        }
    }

    // 3. 플레이어가 오브젝트 근처에서 멀어졌을 때(나갔을 때)
    void OnTriggerExit2D(Collider2D collision)
    {
        // 나간 대상의 태그가 "Player"라면?
        if (collision.CompareTag("Player"))
        {
            textObject.SetActive(false); // 텍스트 끄기!
        }
    }
}