package main

import (
	"bufio"
	"fmt"
	"net"
	"os"
	"strings"
	"time"
)

func main() {
	args := os.Args[1:]

	var path string
	if len(args) == 1 {
		path = args[0]
	} else {
		path = "/"
	}

	host := "127.0.0.1:8080"
	timeout := 2 * time.Second

	conn, err := net.DialTimeout("tcp", host, timeout)
	if err != nil {
		os.Exit(1)
	}
	defer conn.Close()

	req := fmt.Sprintf("GET %s HTTP/1.0\r\nHost: localhost\r\n\r\n", path)
	conn.SetWriteDeadline(time.Now().Add(timeout))
	_, err = conn.Write([]byte(req))
	if err != nil {
		os.Exit(1)
	}

	conn.SetReadDeadline(time.Now().Add(timeout))
	r := bufio.NewReader(conn)
	statusLine, err := r.ReadString('\n')
	if err != nil {
		os.Exit(1)
	}
	statusLine = strings.TrimSpace(statusLine)

	// Expect: HTTP/1.1 200 OK
	if strings.HasPrefix(statusLine, "HTTP/") {
		parts := strings.SplitN(statusLine, " ", 3)
		if len(parts) >= 2 {
			code := parts[1]
			if len(code) > 0 && (code[0] == '2' || code[0] == '3') {
				os.Exit(0)
			}
		}
	}

	// Any other response is unhealthy
	fmt.Fprintln(os.Stderr, "unhealthy status:", statusLine)
	os.Exit(1)
}
