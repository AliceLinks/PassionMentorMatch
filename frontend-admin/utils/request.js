// ==================== 配置区域 ====================
// 调用模式：'container' 使用云托管，'http' 使用传统 HTTP 请求
const USE_CLOUD_CONTAINER = true; // 改为 false 使用 HTTP 模式（本地调试）

// 云托管配置
const CLOUD_ENV = 'prod-8glbi5hp12efd72e'; // 你的云环境 ID
const SERVICE_NAME = 'mentor-backend'; // 你的服务名称

// HTTP 模式配置（本地调试用）
let BASE_URL = 'http://10.6.18.22:8080';
try {
  const stored = wx.getStorageSync('BASE_URL');
  if (stored && typeof stored === 'string' && stored.trim().length > 0) {
    BASE_URL = stored.trim();
  }
} catch (e) { }
// ==================== 配置区域结束 ====================

// 云托管请求方法
const callContainer = (path, method = 'GET', data = {}) => {
  const token = wx.getStorageSync('token');

  // 处理 GET 请求参数：拼接到 URL
  let fullPath = path;
  if (method === 'GET' && data && Object.keys(data).length > 0) {
    const queryString = Object.keys(data)
      .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(data[key])}`)
      .join('&');
    fullPath = path + (path.indexOf('?') > -1 ? '&' : '?') + queryString;
  }

  return new Promise((resolve, reject) => {
    wx.cloud.callContainer({
      config: {
        env: CLOUD_ENV
      },
      path: fullPath,
      method: method,
      header: {
        'X-WX-SERVICE': SERVICE_NAME,
        'Content-Type': 'application/json',
        ...(token ? { 'Authorization': `Bearer ${token}`, 'X-Auth-Token': token } : {})
      },
      data: method === 'GET' ? undefined : data,
      success: (res) => {
        if (res.statusCode === 401) {
          wx.navigateTo({ url: '/pages/admin/login/index' });
          reject(res);
        } else if (res.data.code === 200) {
          resolve(res.data.data);
        } else {
          wx.showToast({ title: res.data.message || '请求失败', icon: 'none' });
          reject(res.data);
        }
      },
      fail: (err) => {
        console.error('云托管调用失败:', err);
        wx.showToast({ title: err.errMsg || '网络错误', icon: 'none' });
        reject(err);
      }
    });
  });
};

// HTTP 请求方法（本地调试用）
const httpRequest = (url, method = 'GET', data = {}) => {
  const token = wx.getStorageSync('token');
  const basicAuth = wx.getStorageSync('BASIC_AUTH');
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${BASE_URL}${url}`,
      method,
      data,
      header: (() => {
        const h = { 'Content-Type': 'application/json' };
        if (basicAuth && typeof basicAuth === 'string' && basicAuth.startsWith('Basic ')) {
          h['Authorization'] = basicAuth;
        } else if (token) {
          h['Authorization'] = `Bearer ${token}`;
        }
        if (token) {
          h['X-Auth-Token'] = token;
        }
        return h;
      })(),
      success: (res) => {
        if (res.statusCode === 401) {
          wx.navigateTo({ url: '/pages/admin/login/index' });
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

// 统一请求入口：根据配置自动选择云托管或 HTTP 模式
const request = (url, method = 'GET', data = {}) => {
  if (USE_CLOUD_CONTAINER) {
    return callContainer(url, method, data);
  } else {
    return httpRequest(url, method, data);
  }
};

module.exports = {
  get: (url, data) => request(url, 'GET', data),
  post: (url, data) => request(url, 'POST', data),
  put: (url, data) => request(url, 'PUT', data),
  del: (url, data) => request(url, 'DELETE', data)
};