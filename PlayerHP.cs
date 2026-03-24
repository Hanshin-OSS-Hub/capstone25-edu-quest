using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using TMPro; 

public class PlayerHP : MonoBehaviour
{
    public int maxHp = 100;       
    public int currentHp;         
    
    public TextMeshProUGUI hpText; 

    // --- [무적 기능 관련 변수 추가] ---
    public float invincibleTime = 3f; // 무적 시간 (기본값 3초)
    private bool isInvincible = false; // 현재 무적 상태인지 확인하는 스위치 (true면 무적)
    Animator anim;

    void Start()
    {
        anim = GetComponent<Animator>();
        currentHp = maxHp; 
        UpdateHpUI();      
    }

    void UpdateHpUI()
    {
        if (hpText != null)
        {
            hpText.text = "HP : " + currentHp + " / " + maxHp;
        }
    }


    // 몬스터와 충돌했을 때 실행되는 함수
    public void TakeDamage(int damage)
    {
        // 1. 만약 지금 무적 상태(true)라면? 
        // return을 만나서 아래 코드(체력 깎이는 코드)를 실행하지 않고 바로 끝냅니다!
        if (isInvincible == true) 
        {
            return; 
        }

       

        // 2. 무적 상태가 아니라면 체력을 깎습니다.
        currentHp -= damage;
        
        if (currentHp < 0) 
        {
            currentHp = 0;
            Debug.Log("플레이어 사망!");
        }
        
        UpdateHpUI(); 

        // 3. 데미지를 한 번 입었으니 무적 상태로 만듭니다.
        isInvincible = true;

        // 4. 설정한 시간(3초) 뒤에 EndInvincible 함수를 실행해서 무적을 풀어줍니다.
        Invoke("EndInvincible", invincibleTime);
    }

    // 무적 상태를 끝내는 함수
    void EndInvincible()
    {
        isInvincible = false; // 무적 스위치 끄기
    
    }
}