# GridPasswordView

## 运行效果图：    

![运行效果图](https://github.com/ttarfall/GridPasswordView/blob/master/gif/Gif_174742.gif?raw=true)

这里是继承TextView实现，相对来说性能非常好

同事做了适配，保证  [GridPasswordView](https://github.com/ttarfall/GridPasswordView) 保证适配

* 对于一些变态的需求要求每个密码框式正方形也做了适配

* 支持定义密码框长度，密码可以明文或者密码显示

* 根据android的inputType类型来确认

* 增加闪动光标，可自定义样式

![运行效果图](https://github.com/ttarfall/GridPasswordView/blob/master/gif/GridPassWordViewDemo.png?raw=true)

xml：
```
 <com.ucsmy.gridpassword.UCSGridPasswordView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp"
        android:cursorVisible="true"
        android:inputType="numberPassword"
        android:padding="8dp"
        android:textSize="18sp"
        app:gpCursorDrawable="@drawable/my_cursor"
        app:gpLineColor="@color/primary"
        app:gpLineHeight="40dp"
        app:gpLineWidth="2dp"
        app:gpPasswordLength="6" />
```

## 最后再贴一张效果图
![运行效果图](https://github.com/ttarfall/GridPasswordView/blob/master/gif/Gif_175028.gif?raw=true)

有问题可以留言保证及时更新
