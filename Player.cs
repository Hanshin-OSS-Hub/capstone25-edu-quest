using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using System;
using UnityEngine.SceneManagement;
using UnityEngine.UI;

public class Player : MonoBehaviour
{
    public float maxSpeed;
    public float jumpPower;
    public int code = 0;
    public GameObject nextPotal;
    Rigidbody2D rigid;
    SpriteRenderer sprend;
    Animator anim;
    AudioSource item_audio;

    void Awake()
    {
        rigid = GetComponent<Rigidbody2D>();
        sprend = GetComponent<SpriteRenderer>();
        anim = GetComponent<Animator>();
        item_audio = GetComponent<AudioSource>();
    }

    void FixedUpdate()
    {
        float h = Input.GetAxis("Horizontal");
        rigid.AddForce(Vector2.right * h, ForceMode2D.Impulse);

        if(rigid.velocity.x > maxSpeed) //오른쪽 이동
        {
            rigid.velocity = new Vector2(maxSpeed, rigid.velocity.y);
        }
        else if(rigid.velocity.x < -maxSpeed)   //왼쪽 이동
        {
            rigid.velocity = new Vector2(-maxSpeed, rigid.velocity.y);
        }
    }

    void Update()
    {
        if(Input.GetButtonDown("Horizontal") && !anim.GetBool("isJump"))
        {
            sprend.flipX = Input.GetAxisRaw("Horizontal") < 0;  //왼쪽 이동시 스프라이트 좌우 반전
        }
        else if (anim.GetBool("isJump"))
        {
            sprend.flipX = Input.GetAxisRaw("Horizontal") > 0; //점프 중일 때 이동 방향에 따라 스프라이트 좌우 반전
        }

        if(rigid.velocity.normalized.x == 0)
        {
            anim.SetBool("isRun", false);
        } // 속도 단위 벡터 값이 0이면 정지.
        else
        {
            anim.SetBool("isRun", true);
        }

        if(Input.GetButtonDown("Jump") && !(anim.GetBool("isJump")))
        {
            rigid.AddForce(Vector2.up * jumpPower, ForceMode2D.Impulse);
        }

        if(Mathf.Abs(rigid.velocity.y) <0.1)  // 속도 값이 0에 가까우면 isJump false
        {
            anim.SetBool("isJump", false);
        }
        else
        {
            anim.SetBool("isJump", true);
        }
    }

    private void OnTriggerEnter2D(Collider2D collision)
    {
        if(collision.gameObject.tag == "Code")
        {
            item_audio.Play();
            collision.gameObject.SetActive(false);
            code++;

            if(code >= 6)
            {
                ActivatePotal();
            }
        }

        else if(collision.tag == "Potal")
        {
            SceneManager.LoadScene("Exam");
        }

        void ActivatePotal()
        {
            nextPotal.SetActive(true);
        }
    }
}
