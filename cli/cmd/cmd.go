package cmd

import (
	"fmt"
	"os"

	"github.com/spf13/cobra"
)

var rootCmd = &cobra.Command{
	Use:   "protoman",
	Short: "Protoman is a fantastic way of managing your protos",
	Run: func(cmd *cobra.Command, args []string) {
		// Do Stuff Here
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
	Run: func(cmd *cobra.Command, args []string) {
		fmt.Println("Publishing...")
	},
}

// Execute CLI
func Execute() {
	if err := rootCmd.Execute(); err != nil {
		fmt.Println(err)
		os.Exit(1)
	}
}
