// 生产/联调：指向云托管域名；如需本地联调改回 http://127.0.0.1:8080/api
// 优先使用本地存储的 BASE_URL，便于在开发者工具中快速切换服务域名
let BASE_URL = 'http://10.6.70.199:8080/api';
try {
  const stored = wx.getStorageSync('BASE_URL');
  if (stored && typeof stored === 'string' && stored.trim().length > 0) {
    BASE_URL = stored.trim();
  }
} catch (e) {}

const request = (url, method = 'GET', data = {},options = {}) =>{
  // options 可以传 { tokenKey: 'ADMIN_TOKEN' } 来读取管理员 token
  const tokenKey = options.tokenKey || 'token';
  const token = wx.getStorageSync(tokenKey);
  const basicAuth = wx.getStorageSync('BASIC_AUTH'); // 形如 "Basic xxxxx"，仅在开启 BasicAuth 的网关场景使用
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${BASE_URL}${url}`,
      method,
      data,
      header: (() => {
        const h = { 'Content-Type': 'application/json' };
        // 若设置了 BASIC_AUTH，则优先用于网关鉴权；应用内令牌改用 X-Auth-Token 传递
        if (basicAuth && typeof basicAuth === 'string' && basicAuth.startsWith('Basic ')) {
          h['Authorization'] = basicAuth;
        } else if (token) {
          h['Authorization'] = `Bearer ${token}`;
        }
        if (token) {
          h['X-Auth-Token'] = token; // 兼容后端在 Basic 模式下从自定义头读取令牌
        }
        return h;
      })(),
      success: (res) => {
        if (res.statusCode === 401) {
          // Token 失效，跳转登录
          wx.navigateTo({ url: '/pages/login/index' });
          reject(res);
        } else if (res.data.code === 200) {
          resolve(res.data.data);
        } else {
          wx.showToast({ title: res.data.message || '请求失败', icon: 'none' });
          reject(res.data);
        }
      },
      fail: (err) => {
        wx.showToast({ title: '网络错误', icon: 'none' });
        reject(err);
      }
    });
  });
};

module.exports = {
  get: (url, data) => request(url, 'GET', data),
  post: (url, data) => request(url, 'POST', data),
  put: (url, data) => request(url, 'PUT', data),
  del: (url, data) => request(url, 'DELETE', data)
};