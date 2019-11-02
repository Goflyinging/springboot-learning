package com.lxing.feign.spring.bean;

import com.lxing.feign.annotation.Rest;

public class TestBean {

  @Rest(url = "https://api.github.com")
  private GitHub gitHub;

  public GitHub getGitHub() {
    return gitHub;
  }

  public void setGitHub(GitHub gitHub) {
    this.gitHub = gitHub;
  }
}
