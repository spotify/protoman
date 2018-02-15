package cmd

import (
	"errors"
	"fmt"
	"os"

	"github.com/spf13/cobra"
	"github.com/spotify/protoman/cli/publish"
)

var rootCmd = &cobra.Command{
	Use:   "protoman",
	Short: "Protoman is a fantastic way of managing your protos",
	Run: func(cmd *cobra.Command, args []string) {
		fmt.Println("PROTOMAN PROTOMAN!")
		fmt.Println("You probably need help, call me with -h")
	},
}

func init() {
	rootCmd.AddCommand(versionCmd)
	rootCmd.AddCommand(publishCmd)
}

var versionCmd = &cobra.Command{
	Use:   "version",
	Short: "Print the version number of protoman",
	Run: func(cmd *cobra.Command, args []string) {
		fmt.Println("Protoman version 0.0.1")
	},
}

var publishCmd = &cobra.Command{
	Use:   "publish [path]",
	Short: "Publish your proto(s) to the protoman registry",
	Args: func(cmd *cobra.Command, args []string) error {
		if len(args) < 1 {
			return errors.New("Path required")
		}
		return nil
	},
	Run: func(cmd *cobra.Command, args []string) {
		if _, err := os.Stat(args[0]); err == nil {
			publish.Publish(args[0])
		}
	},
}

// Execute CLI
func Execute() {
	if err := rootCmd.Execute(); err != nil {
		fmt.Println(err)
		os.Exit(1)
	}
}
