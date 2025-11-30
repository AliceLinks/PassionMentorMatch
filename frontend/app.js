App({
    onLaunch() {
      this.isLogging = false;
      this.doLogin();
      const u = wx.getStorageSync('userInfo') || {
        avatar: 'https://mmbiz.qpic.cn/mmbiz/icTdbqWNOwNRna42FI242Lcia07jQodd2FJGIYQfG0LAJGFxM4FbnQP6yfMxBgJ0F3YRqJCJ1aPAK2dQagdusBZg/0',
        nickname: '游客'
      };
      this.globalData.user = u;
      this.globalData.language = 'zh';
    },
  
    doLogin(retryFlag = false) {
      return new Promise((resolve, reject) => {
        const cached = wx.getStorageSync('token');
        if (cached) { resolve(cached); return; }
  
        if (this.isLogging) { reject('登录进行中'); return; }
        this.isLogging = true;
        wx.showLoading({ title: '登录中', mask: true });
  
        wx.login({
          success: (res) => {
            if (!res.code) {
              reject('获取 code 失败: ' + res.errMsg);
              return;
            }
            this.globalData.loginCode = res.code;
            wx.request({
              url: 'http://127.0.0.1:8080/api/user/login', // 后端接口
              method: 'POST',
              header: { 'content-type': 'application/json' },
              data: { code: res.code },
              timeout: 8000,
              success: (response) => {
                const { statusCode, data } = response;
                if (statusCode === 200 && data && data.code === 200 && data.data && data.data.token) {
                  const token = data.data.token;
                  wx.setStorageSync('token', token);
                  console.log('Token已获取并存储:', token);
                  resolve(token);
                } else {
                  // 后端 msg 或默认信息
                  const msg = (data && data.msg) || '登录失败';
                  // 如果第一次失败且还未重试，自动重试一次
                  if (!retryFlag) {
                    console.warn('登录失败，尝试自动重试一次:', msg);
                    this.isLogging = false;
                    this.doLogin(true).then(resolve).catch(reject);
                  } else {
                    reject(msg);
                  }
                }
              },
              fail: (err) => {
                if (!retryFlag) {
                  console.warn('网络失败，自动重试一次');
                  this.isLogging = false;
                  this.doLogin(true).then(resolve).catch(reject);
                } else {
                  reject('网络错误');
                }
              }
            });
          },
          fail: (e) => reject('wx.login 调用失败: ' + e.errMsg)
        });
      }).catch(err => {
        console.error('登录流程失败:', err);
        wx.showToast({ title: '登录失败', icon: 'none' });
        throw err;
      }).finally(() => {
        this.isLogging = false;
        wx.hideLoading();
      });
    },
  
    globalData: { user: null, language: 'zh', mapKey: 'G42BZ-6LWLT-JVXXV-LQWMS-JCD2H-JCFJM', loginCode: '' }
  });