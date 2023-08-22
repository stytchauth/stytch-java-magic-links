<!DOCTYPE html>
<html lang="en">
    <head>
        <title>Hello Socks | Stytch example</title>
        <link rel="stylesheet" href="/static/css/styles.css">
    </head>
    <body>
        <div class="app">
            <div class="nav">
                <h2>Hello Socks</h2>
                <div>
                    <span class="navItem">Women</span>
                    <span class="navItem">Men</span>
                    <span class="navItem">Kids</span>
                    <span class="navItem">Sale</span>
                    <span class="navItem">Community</span>
                </div>
                <div>
                    <img class="icon" src="/static/images/shopping-bag.png">
                    <a href="/profile"><img class="icon" src="/static/images/person.png"></a>
                </div>
            </div>
            <div class="content profile">
                <h1>User Details</h1>
                <p><strong>User ID:</strong> ${user.userId}</p>
                <p><strong>Email Address:</strong> ${user.emails[0].email}</p>
                <p><strong>Name:</strong> ${user.name.firstName} ${user.name.lastName}</p>
                <p><strong><a href="/logout">Log Out</a></strong></p>
            </div>
        </div>
    </body>
</html>
