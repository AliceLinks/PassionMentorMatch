const api = require('./utils/request.js');

App({
    onLaunch() {
      this.isLogging = false;
      this.loginPromise = null; // 复用进行中的登录承诺，避免并发
      this._loadingShown = false;
      this.doLogin();
      const u = wx.getStorageSync('userInfo') || {
        avatar: 'https://mmbiz.qpic.cn/mmbiz/icTdbqWNOwNRna42FI242Lcia07jQodd2FJGIYQfG0LAJGFxM4FbnQP6yfMxBgJ0F3YRqJCJ1aPAK2dQagdusBZg/0',
        nickname: '游客'
      };
      this.globalData.user = u;
      this.globalData.language = 'zh';
    },
  
    doLogin(retryFlag = false) {
      // 若已有进行中的登录，复用该 Promise
      if (this.loginPromise) return this.loginPromise;
      this.loginPromise = new Promise((resolve, reject) => {
        const cached = wx.getStorageSync('token');
        if (cached) { resolve(cached); return; }

        if (!this._loadingShown) { wx.showLoading({ title: '登录中', mask: true }); this._loadingShown = true; }
        this.isLogging = true;

        wx.login({
          success: (res) => {
            if (!res.code) {
              reject('获取 code 失败: ' + res.errMsg);
              return;
            }
            this.globalData.loginCode = res.code;
            api.post('/user/login', { code: res.code })
              .then(data => {
                if (data && data.token) {
                  const token = data.token;
                  wx.setStorageSync('token', token);
                  console.log('Token已获取并存储:', token);
                  resolve(token);
                } else {
                  const msg = '登录失败';
                  if (!retryFlag) {
                    console.warn('登录失败，尝试自动重试一次:', msg);
                    this.isLogging = false;
                    // 清空以允许重试创建新 Promise
                    this.loginPromise = null;
                    this.doLogin(true).then(resolve).catch(reject);
                  } else {
                    reject(msg);
                  }
                }
              })
              .catch(err => {
                if (!retryFlag) {
                  console.warn('网络失败，自动重试一次');
                  this.isLogging = false;
                  this.loginPromise = null;
                  this.doLogin(true).then(resolve).catch(reject);
                } else {
                  reject('网络错误');
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
        if (this._loadingShown) { wx.hideLoading(); this._loadingShown = false; }
        // 清空以便后续需要时可重新登录
        this.loginPromise = null;
      });
      return this.loginPromise;
    },
  
    globalData: { user: null, language: 'zh', mapKey: 'G42BZ-6LWLT-JVXXV-LQWMS-JCD2H-JCFJM', loginCode: '' }
  });