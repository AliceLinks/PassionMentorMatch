Page({
  data: { date:'', start:'', end:'', teacher:'', dance:'', capacity:'', items:[] },
  onDate(e){ this.setData({ date: e.detail.value }); },
  onStart(e){ this.setData({ start: e.detail.value }); },
  onEnd(e){ this.setData({ end: e.detail.value }); },
  onDatePick(e){ this.setData({ date: e.detail.value }); },
  onStartPick(e){ this.setData({ start: e.detail.value }); },
  onEndPick(e){ this.setData({ end: e.detail.value }); },
  onTeacher(e){ this.setData({ teacher: e.detail.value }); },
  onDance(e){ this.setData({ dance: e.detail.value }); },
  onCapacity(e){ this.setData({ capacity: e.detail.value }); },
  /**
   * 校验单个课程项的表单字段
   */
  validateForm(form) {
    // 必填校验（使用后端字段名做统一校验，以避免混淆）
    if (!form.course_date || !form.start_time || !form.end_time || !form.teacher || !form.dance_type || (form.capacity === undefined || form.capacity === null)) {
      return { valid: false, message: '请填写完整的课程日期、开始/结束时间、教师、舞蹈类型和容量' };
    }

    // 日期不得早于今天
    const today = new Date();
    const yyyy = today.getFullYear();
    const mm = (today.getMonth() + 1).toString().padStart(2, '0');
    const dd = today.getDate().toString().padStart(2, '0');
    const todayStr = `${yyyy}-${mm}-${dd}`;
    if (form.course_date < todayStr) {
      return { valid: false, message: '日期不能早于今天' };
    }

    // 结束时间必须晚于开始时间
    const start = form.start_time;
    const end = form.end_time;
    if (start && end && start >= end) {
      return { valid: false, message: '结束时间必须晚于开始时间' };
    }

    // 容量为正整数
    const cap = Number(form.capacity);
    if (!Number.isInteger(cap) || cap <= 0) {
      return { valid: false, message: '容量必须为正整数' };
    }

    return { valid: true };
  },
  addItem(){
    const { date, start, end, teacher, dance, capacity } = this.data;
    const item = {
      course_date: date,
      start_time: start,
      end_time: end,
      teacher,
      dance_type: dance,
      capacity: Number(capacity)
    };
    const res = this.validateForm(item);
    if (!res.valid) {
      wx.showToast({ title: res.message, icon: 'none' });
      return;
    }
    const items = this.data.items.slice();
    items.push(item);
    this.setData({ items, date:'', start:'', end:'', teacher:'', dance:'', capacity:'' });
  },
  removeItem(e){
    const idx = e.currentTarget.dataset.index;
    const items = this.data.items.slice();
    items.splice(idx,1);
    this.setData({ items });
  },
  onSubmit(){
    const base = wx.getStorageSync('BASE_URL');
    if(!base){ wx.showToast({ title:'未配置BASE_URL', icon:'none' }); return; }
    if(!this.data.items.length){ wx.showToast({ title:'请先添加课程', icon:'none' }); return; }

    // 对所有项做提交前校验
    for (let i = 0; i < this.data.items.length; i++) {
      const res = this.validateForm(this.data.items[i]);
      if (!res.valid) {
        wx.showToast({ title: `第${i + 1}项：${res.message}`, icon: 'none' });
        return;
      }
    }

    // 兼容 BASE_URL 可能已包含 /api，避免出现 /api/api 重复
    const apiPrefix = base.endsWith('/api') ? base : `${base}/api`;
    wx.request({
      url: `${apiPrefix}/courses/batch`,
      method: 'POST',
      // 后端期望直接接收数组，而非 {items: [...]} 包裹对象
      data: this.data.items,
      header: {
        'content-type': 'application/json',
        'Authorization': `Bearer ${wx.getStorageSync('ADMIN_TOKEN')}`
      },
      success: (res) => {
        // 调试输出：便于定位响应体与判定差异
        console.log('batch.publish response', {
          statusCode: res.statusCode,
          type: typeof res.data,
          data: res.data
        });
        // const debugMsg = `status:${res.statusCode} code:${res?.data?.code ?? 'n/a'} msg:${res?.data?.message ?? 'n/a'}`;
        // wx.showToast({ title: debugMsg.slice(0, 28), icon: 'none' });

        const ok = res.statusCode === 200 || res.statusCode === 201;
        if (!ok) { wx.showToast({ title: '提交失败', icon: 'none' }); return; }

        // 后端可能直接返回空响应体或非标准结构，视为成功
        if (!res.data || typeof res.data !== 'object') {
          wx.showToast({ title: '发布成功', icon: 'success' });
          this.setData({ items: [] });
          return;
        }

        // 兼容不同的返回格式
        const code = res.data.code;
        const message = res.data.message;
        const payload = res.data.data;
        // 明确优先成功规则：HTTP 200/201 且 code===200 或 message==='success' 即成功
        if (code === 200 || message === 'success') {
          const successCount = payload?.success ?? 0;
          const failCount = payload?.failed ?? 0;
          if (successCount || failCount) {
            wx.showToast({ title: `成功:${successCount} 失败:${failCount}`, icon:'success' });
          } else {
            wx.showToast({ title: '发布成功', icon:'success' });
          }
          this.setData({ items: [] });
          return;
        }
        if (typeof code === 'undefined') {
          wx.showToast({ title: '发布成功', icon:'success' });
          this.setData({ items: [] });
          return;
        }
        if (code === 0) {
          if (payload && (typeof payload.success !== 'undefined' || typeof payload.failed !== 'undefined')) {
            const successCount = payload.success ?? 0;
            const failCount = payload.failed ?? 0;
            wx.showToast({ title: `成功:${successCount} 失败:${failCount}`, icon:'success' });
          } else if (Array.isArray(payload)) {
            wx.showToast({ title: `成功:${payload.length}`, icon:'success' });
          } else {
            wx.showToast({ title: '发布成功', icon:'success' });
          }
          this.setData({ items: [] });
        } else {
          const msg = (res.data && res.data.message) || '提交失败';
          // 若后端未提供 message，但 HTTP 已成功，仍按成功处理
          if (!res.data.message) {
            wx.showToast({ title: '发布成功', icon: 'success' });
            this.setData({ items: [] });
          } else {
            wx.showToast({ title: msg, icon:'none' });
          }
        }
      },
      fail: () => { wx.showToast({ title:'网络错误', icon:'none' }); }
    });
  }
      ,
      cancelCourse(e){
        const id = e.currentTarget.dataset.id;
        if(!id){ wx.showToast({ title:'缺少课程ID', icon:'none' }); return; }
        const base = wx.getStorageSync('BASE_URL');
        const apiPrefix = base && base.endsWith('/api') ? base : `${base}/api`;
        wx.showModal({
          title:'确认取消',
          content:'取消后将无法预约该课程，是否继续？',
          success:(d)=>{
            if(!d.confirm) return;
            wx.request({
              url:`${apiPrefix}/courses/${id}/cancel`,
              method:'POST',
              header:{ 'content-type':'application/json', 'Authorization': `Bearer ${wx.getStorageSync('ADMIN_TOKEN')}` },
              success:(res)=>{
                if(res.statusCode===200){
                  wx.showToast({ title:'已取消', icon:'success' });
                  // 可选：从本地 items 移除或刷新列表
                  const items = (this.data.items||[]).map(x=>{ if(x.id===id){ x.status='cancelled'; } return x; });
                  this.setData({ items });
                } else {
                  wx.showToast({ title: res.data?.message || '取消失败', icon:'none' });
                }
              },
              fail:()=> wx.showToast({ title:'网络错误', icon:'none' })
            });
          }
        });
      }
});