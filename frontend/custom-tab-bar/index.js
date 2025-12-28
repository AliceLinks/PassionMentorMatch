Component({
  data: {
    visible: true,
    selected: 0,
    list: [
      { pagePath: "/pages/home/index", text: "主页", icon: "/custom-tab-bar/icons/home.png", selectedIcon: "/custom-tab-bar/icons/home-active.png" },
      { pagePath: "/pages/course/index", text: "约课", icon: "/custom-tab-bar/icons/calendar.png", selectedIcon: "/custom-tab-bar/icons/calendar-active.png" },
      { pagePath: "/pages/profile/index", text: "我的", icon: "/custom-tab-bar/icons/user.png", selectedIcon: "/custom-tab-bar/icons/user-active.png" }
    ]
  },
  attached() {
    console.log('tabBar attached');
    this.updateSelected();
  },
  pageLifetimes: {
    show() {
      console.log('tabBar show');
      this.updateSelected();
    }
  },
  methods: {
    onSwitch(e) {
      console.log('tabBar onSwitch', e);
      const idx = Number(e.currentTarget.dataset.index);
      const item = this.data.list[idx];
      wx.switchTab({ url: item.pagePath });
    },
    updateSelected() {
      const pages = getCurrentPages();
      if (!pages || pages.length === 0) {
        this.setData({ selected: 0 });
        return;
      }
      const currentPage = pages[pages.length - 1];
      let route = currentPage.route; // 例如 "pages/home/index"

      /* ========= 关键点 1：admin 页面直接隐藏 tabBar ========= */
      if (route.startsWith('pages/admin/')) {
        this.setData({ visible: false });
        return;
      }

      /* ========= 关键点 2：用户端页面才显示 tabBar ========= */
      this.setData({ visible: true });
      
      const list = this.data.list;
      console.log('updateSelected 调试 route:', route);
      console.log('updateSelected 调试 list:', list);
      let selected = 0;
      for (let i = 0; i < list.length; i++) {
        console.log('对比 route:', route, '与 pagePath:', list[i].pagePath);
        if (route === list[i].pagePath) {
          selected = i;
          break;
        }
      }
      this.setData({ selected });
    }
  }
})